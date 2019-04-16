#!/usr/bin/env groovy

import java.util.regex.Pattern
import java.util.regex.Matcher
import groovy.json.JsonOutput
import net.sf.json.JSONObject

String determineOrgName(url) {
    def finder = (url =~ /:([^:]*)\//)
    return finder.getAt(0).getAt(1)
}

String determineRepoName(url) {
    return scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
}

Map findBuildDef(id, buildDefinitions) {
    return buildDefinitions.find { it.id == id }
}

Map findAndValidateTargetBuild(id, buildDefinitions) {
    logger "Finding Build Definition with id ${id}"
    def result = findBuildDef(id, buildDefinitions)
    def targetBuild = result.clone()
    validateBuildDef(targetBuild)
    removeCustomPropertiesFromBuildDef(targetBuild)

    return targetBuild
}

// get a list of all the buildIds after checking if optional builds are specified by github pr labels
List<String> getBuildIdsWithOptional(config) {
    logger "Evaluating Github PR Labels..."
    withCredentials([string(credentialsId: config.gitTokenId, variable: 'TOKEN')]) {
        // determine base repo/branch git url info
        def url = scm.getUserRemoteConfigs()[0].getUrl()
        def orgName = determineOrgName(url)
        def repoName = determineRepoName(url)

        // get latest commit hash from the current branch
        def branchInfoUrl = "https://github.build.ge.com/api/v3/repos/${orgName}/${repoName}/branches/${BRANCH_NAME}"
        def branchInfoResponse = httpRequest(url: branchInfoUrl, customHeaders: [[maskValue: false, name: 'Authorization', value: "token ${TOKEN}"]])
        def branchInfo = readJSON(text: branchInfoResponse.content)
        def latestCommit = branchInfo.commit.sha
        logger "Latest ${BRANCH_NAME} branch commit: ${latestCommit}"

        // find the pr with this merge_hash
        def prListUrl = "https://github.build.ge.com/api/v3/repos/${orgName}/${repoName}/pulls?state=closed"
        def prListResponse = httpRequest(url: prListUrl, customHeaders: [[maskValue: false, name: 'Authorization', value: "token ${TOKEN}"]])
        def prList = readJSON(text: prListResponse.content)
        def targetPr = prList.find { it.merge_commit_sha == latestCommit }
        logger "PR found matching commit ${latestCommit} . PR:${targetPr.number}"
        
        // determine if the pr has any labels
        def buildIds = []
        if (targetPr.labels.size() > 0) {
            def labelIds = targetPr.labels.collect {
                if (it.name.startsWith("build:")) {
                    return it.name.replace("build:", "")
                }
            }.minus(null)
            
            // iterate through builds and add ids that pass optional check
            buildIds = config.downstreamBuilds[BRANCH_NAME].collect {
                if (!it.optional || labelIds.contains(it.id)) {
                    return it.id
                }
            }.minus(null)
        } else {
            // just filter out any listed as optional since no labels were found
            logger "No labels found for the associated pr. No optional downstream builds will be triggered"
            buildIds = config.downstreamBuilds[BRANCH_NAME].collect {
                if (!it.optional) {
                    return it.id
                }
            }.minus(null)
        }

        return buildIds
    }
}

// remove any custom properties that we support which we know
// aren't properties of the 'jenkins pipeline build step' https://jenkins.io/doc/pipeline/steps/pipeline-build-step/
def removeCustomPropertiesFromBuildDef(build) {
    build.remove('id')
    build.remove('optional')
    build.remove('parallel')
}

def validateBuildDef(build) {
    def missingProps = []
    def requiredProps = ['job', 'id']

    requiredProps.each { 
        if (!build.containsKey(it)) { 
            missingProps.add(it) 
        }
    }

    if (missingProps.size() > 0) {
        currentBuild.result = 'ABORTED'
        error("Downstream Build Definition is missing the required field(s): ${missingProps.join(',')}")
    }
}

def executeBuilds(buildIds, downstreamBuildDefinitions) {
    def nextBuild = findBuildDef(buildIds.get(0), downstreamBuildDefinitions)

    if (buildIds.size() > 0 && nextBuild.parallel) {
        logger "Executing Downstream Builds in parallel"
        executeParallelBuilds(buildIds, downstreamBuildDefinitions)
    } else {
        logger "Executing Downstream Builds in serial"
        executeSerialBuild(buildIds, downstreamBuildDefinitions)
    }
}

def executeSerialBuild(buildIds, downstreamBuildDefinitions) {
    def targetBuildId = buildIds.removeAt(0)
    def targetBuild = findAndValidateTargetBuild(targetBuildId, downstreamBuildDefinitions)

    logger "Downstream Build Located: ${targetBuild.job}"
    if (buildIds.size() == 0) {
        buildIds.add("THE_END")
    }
    
    def buildDefaults = [
        propagate: false,
        wait: false
    ]

    def buildParams = [
        string(name: 'downstreamBuildIds', value: buildIds.join(',')),
        string(name: 'downstreamBuildDefinitions', value: JsonOutput.toJson(downstreamBuildDefinitions))
    ]

    if (targetBuild.parameters) {
        buildParams = (targetBuild.parameters + buildParams)
    }

    // this syntax allows the 'jenkins pipeline build step' to add properties 
    // in the future and automatically be support with-out code change. (unless they use a prop name we're using, ie) 'id', 'optional'
    build(buildDefaults << targetBuild << [parameters: buildParams])
}

// have to write this abomination because we can't use takeWhile() on jenkins cause of CPS
def getParallelBuildIds(buildIds, downstreamBuildDefinitions) {
    // get all consecutive buildIds which map to definitions that have `parallel: true`=
    def parallelBuildIds = []
    def falseSeen = false
    buildIds.each {
        def result = findBuildDef(it, downstreamBuildDefinitions).parallel
        if (!result && !falseSeen) {
            falseSeen = true
        }
        if (result && !falseSeen) {
            parallelBuildIds.add(it)
        }
    }
    
    return parallelBuildIds
}

def executeParallelBuilds(buildIds, downstreamBuildDefinitions) {
    def parallelBuildIds = getParallelBuildIds(buildIds, downstreamBuildDefinitions)
    logger "Parallel Build IDs identified: ${parallelBuildIds.join(',')}"

    // calculate the remaining build ids after the parallel builds run
    def remainingBuildIds = buildIds.drop(parallelBuildIds.size())

    // assemble our parallel builds
    def parallelBuilds = [:]
    parallelBuildIds.each {
        def targetBuild = findAndValidateTargetBuild(it, downstreamBuildDefinitions)

        def buildDefaults = [
            propagate: false,
            wait: false
        ]

        def buildParams = (buildDefaults << targetBuild)
        // if there will be builds remaining after the parallel builds complete OR
        // buildParams has propagate set to true we ensure `wait = true`
        buildParams.wait = (remainingBuildIds.size() > 0 || buildParams.propogate) ? true : false

        // if the tagetBuild has the 'wait' property we remove it because users aren't allowed to set it on a parrallel job
        targetBuild.remove('wait')
        logger "Scheduling Parallel Build ${it}"
        parallelBuilds["ParallelBuild:${it}"] = { build(buildParams) }
    }
    
    if (remainingBuildIds.size() > 0) {
        // execute our parallel builds
        logger "Will wait for parrallel builds to complete and continue with the remaining builds: ${remainingBuildIds}"
        parallel(parallelBuilds)
        executeSerialBuild(remainingBuildIds, downstreamBuildDefinitions)
    } else {
        logger "Executing parallel builds, will not wait for completion"
        parallel(parallelBuilds)
    }
}

def call(config) {
    stage ('Downstream Builds') {
        
        if (config.downstreamBuildBranches) {
            Pattern pattern = Pattern.compile(config.downstreamBuildBranches)

            if (!(BRANCH_NAME ==~ pattern)) {
                logger "${BRANCH_NAME} does not match the downstreamBuildBranches pattern. Skipping Downstream Builds"
                return
            }
        }

        // check to see if this build is part of an ongoing downstream build chain
        // params.downstreamBuildDefinitions is not the same as config.downstreamBuilds. The BRANCH_NAME has already been taken into account at this point
        // and params.downstreamBuildDefinitions just represents the collection of downstreamBuild definitions
        if (params.downstreamBuildDefinitions != 'empty' && params.downstreamBuildIds != 'empty') {
            def buildIds = params.downstreamBuildIds.split(",").toList()

            if (buildIds.getAt(0) == "THE_END") {
                logger "Downstream Build Chain complete"
            } else {
                // we are currently executing a downstream build which needs to trigger additional downstream build(s)
                logger "Downstream Build Chain detected. Continuing to execute ${params.downstreamBuildIds}"
                def downstreamBuildsJSONArr = readJSON(text: params.downstreamBuildDefinitions)
                // convert json objects to Maps so that .clone() can be called later
                def downstreamBuildsArr = downstreamBuildsJSONArr.collect {
                    def buildDef = [:]

                    it.keySet().each { k -> 
                        buildDef[k] = it[k] 
                    }

                    buildDef
                }

                executeBuilds(buildIds, downstreamBuildsArr)
            }

            return
        }

        // see if we have downstreamBuilds in the config
        def buildIds = []
        if (config.downstreamBuilds[BRANCH_NAME].any { it.optional }) {
            logger "Optional Downstream Builds detected"
            buildIds = getBuildIdsWithOptional(config)
            if (buildIds.size() == 0) {
                return
            }
        } else {
            buildIds = config.downstreamBuilds[BRANCH_NAME].collect { it.id }
        }

        executeBuilds(buildIds, config.downstreamBuilds[BRANCH_NAME])
    }
}