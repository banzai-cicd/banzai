#!/usr/bin/env groovy

import java.util.regex.Pattern
import java.util.regex.Matcher
import groovy.json.JsonOutput
import net.sf.json.JSONObject
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiDownstreamBuildCfg

String determineOrgName(url) {
    def finder = (url =~ /:([^:]*)\//)
    return finder.getAt(0).getAt(1)
}

String determineRepoName(url) {
    return scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
}

Map findBuildCfg(id, List<BanzaiDownstreamBuildCfg> downstreamBuildCfgs) {
    return downstreamBuildCfgs.find { it.id == id }
}

Map findAndValidateTargetBuild(id, List<BanzaiDownstreamBuildCfg> downstreamBuildCfgs) {
    logger "Finding Build Cfg with id ${id}"
    BanzaiDownstreamBuildCfg result = findBuildCfg(id, downstreamBuildCfgs)
    BanzaiDownstreamBuildCfg targetBuild = result.clone(new BanzaiDownstreamBuildCfg())
    validateBuildDef(targetBuild)
    removeCustomPropertiesFromBuildCfg(targetBuild)

    return targetBuild
}

// get a list of all the buildIds after checking if optional builds are specified by github pr labels
List<String> getBuildIdsWithOptional(BanzaiCfg cfg, List<BanzaiDownstreamBuildCfg> downstreamBuildsDefinitions) {
    logger "Evaluating Github PR Labels..."
    withCredentials([string(credentialsId: cfg.gitTokenId, variable: 'TOKEN')]) {
        // determine base repo/branch git url info
        String url = scm.getUserRemoteConfigs()[0].getUrl()
        String orgName = determineOrgName(url)
        String repoName = determineRepoName(url)

        // get latest commit hash from the current branch
        String branchInfoUrl = "https://github.build.ge.com/api/v3/repos/${orgName}/${repoName}/branches/${BRANCH_NAME}"
        def branchInfoResponse = httpRequest(url: branchInfoUrl, customHeaders: [[maskValue: false, name: 'Authorization', value: "token ${TOKEN}"]])
        def branchInfo = readJSON(text: branchInfoResponse.content)
        String latestCommit = branchInfo.commit.sha
        logger "Latest ${BRANCH_NAME} branch commit: ${latestCommit}"

        // find the pr with this merge_hash
        String prListUrl = "https://github.build.ge.com/api/v3/repos/${orgName}/${repoName}/pulls?state=closed"
        def prListResponse = httpRequest(url: prListUrl, customHeaders: [[maskValue: false, name: 'Authorization', value: "token ${TOKEN}"]])
        def prList = readJSON(text: prListResponse.content)
        String targetPr = prList.find { it.merge_commit_sha == latestCommit }
        logger "PR found matching commit ${latestCommit} . PR:${targetPr.number}"
        
        // determine if the pr has any labels
        List<String> buildIds = []
        if (targetPr.labels.size() > 0) {
            List<String> labelIds = targetPr.labels.collect {
                if (it.name.startsWith("build:")) {
                    return it.name.replace("build:", "")
                }
            }.minus(null)
            
            // iterate through builds and add ids that pass optional check
            buildIds = downstreamBuildsDefinitions.collect {
                if (!it.optional || labelIds.contains(it.id)) {
                    return it.id
                }
            }.minus(null)
        } else {
            // just filter out any listed as optional since no labels were found
            logger "No labels found for the associated pr. No optional downstream builds will be triggered"
            buildIds = downstreamBuildsDefinitions.collect {
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
def removeCustomPropertiesFromBuildCfg(build) {
    build.id = null
    build.optional = null
    build.parallel = null
}

def validateBuildDef(build) {
    List<String> missingProps = []
    List<String> requiredProps = ['job', 'id']

    requiredProps.each {
        if (build[it] == null) {
            missingProps.add(it) 
        }
    }

    if (missingProps.size() > 0) {
        currentBuild.result = 'ABORTED'
        error("Downstream Build Cfg is missing the required field(s): ${missingProps.join(',')}")
    }
}

def executeBuilds(buildIds, List<BanzaiDownstreamBuildCfg> downstreamBuildCfgs) {
    BanzaiDownstreamBuildCfg nextBuild = findBuildCfg(buildIds.get(0), downstreamBuildCfgs)

    if (buildIds.size() > 0 && nextBuild.parallel) {
        logger "Executing Downstream Builds in parallel"
        executeParallelBuilds(buildIds, downstreamBuildCfgs)
    } else {
        logger "Executing Downstream Builds in serial"
        executeSerialBuild(buildIds, downstreamBuildCfgs)
    }
}

def executeSerialBuild(List<String> buildIds, List<BanzaiDownstreamBuildCfg> downstreamBuildCfgs) {
    String targetBuildId = buildIds.removeAt(0)
    BanzaiDownstreamBuildCfg targetBuild = findAndValidateTargetBuild(targetBuildId, downstreamBuildCfgs)

    logger "Downstream Build Located: ${targetBuild.job}"
    if (buildIds.size() == 0) {
        buildIds.add("THE_END")
    }
    
    if (!targetBuild.job.startsWith("/")) {
        targetBuild.job = "/${targetBuild.job}"
    }

    BanzaiDownstreamBuildCfg buildDefaults = new BanzaiDownstreamBuildCfg()

    def buildParams = [
        string(name: 'downstreamBuildIds', value: buildIds.join(',')),
        string(name: 'downstreamBuildCfgs', value: JsonOutput.toJson(downstreamBuildCfgs))
    ]

    if (targetBuild.parameters) {
        buildParams = (targetBuild.parameters + buildParams)
    }

    // if the user set propogate to true then ensure that wait is also true
    if (targetBuild.propagate == true) {
        targetBuild.wait = true
    }

    // this syntax allows the 'jenkins pipeline build step' to add properties 
    // in the future and automatically be support with-out code change. (unless they use a prop name we're using, ie) 'id', 'optional'
    build(buildDefaults.asMap() << targetBuild.asMap() << [parameters: buildParams])
}

// have to write this abomination because we can't use takeWhile() on jenkins cause of CPS
List<String> getParallelBuildIds(List<String> buildIds, List<BanzaiDownstreamBuildCfg> downstreamBuildCfgs) {
    // get all consecutive buildIds which map to definitions that have `parallel: true`=
    List<String> parallelBuildIds = []
    def falseSeen = false
    buildIds.each {
        def result = findBuildCfg(it, downstreamBuildCfgs).parallel
        if (!result && !falseSeen) {
            falseSeen = true
        }
        if (result && !falseSeen) {
            parallelBuildIds.add(it)
        }
    }
    
    return parallelBuildIds
}

def executeParallelBuilds(List<String> buildIds, List<BanzaiDownstreamBuildCfg> downstreamBuildCfgs) {
    List<String> parallelBuildIds = getParallelBuildIds(buildIds, downstreamBuildCfgs)
    logger "Parallel Build IDs identified: ${parallelBuildIds.join(',')}"

    // calculate the remaining build ids after the parallel builds run
    List<String> remainingBuildIds = buildIds.drop(parallelBuildIds.size())

    // assemble our parallel builds
    def parallelBuilds = [:]
    parallelBuildIds.each {
        BanzaiDownstreamBuildCfg targetBuild = findAndValidateTargetBuild(it, downstreamBuildCfgs)
        BanzaiDownstreamBuildCfg buildDefaults = new BanzaiDownstreamBuildCfg()

        def buildParams = (buildDefaults.asMap() << targetBuild.asMap())
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
        executeSerialBuild(remainingBuildIds, downstreamBuildCfgs)
    } else {
        logger "Executing parallel builds, will not wait for completion"
        parallel(parallelBuilds)
    }
}

def call(BanzaiCfg cfg) {
    String stageName = 'Downstream Builds'
    // check and see if the current branch matches the cfg
    List<BanzaiDownstreamBuildCfg> downstreamBuildCfgs = findValueInRegexObject(cfg.downstreamBuilds, BRANCH_NAME)
    if (!downstreamBuildCfgs) {
        logger "${BRANCH_NAME} does not match a 'downstreamBuilds' branch pattern. Skipping ${stageName}"
        return
    }
    if (!cfg.gitTokenId) {
        logger "cfg.gitTokenId is required by downstreamBuilds. Please configure. Skipping ${stageName}"
        return
    }

    stage (stageName) {
        // check to see if this build is part of an ongoing downstream build chain
        // params.downstreamBuildCfgs is not the same as cfg.downstreamBuilds. The BRANCH_NAME has already been taken into account at this point
        // and params.downstreamBuildCfgs just represents the collection of downstreamBuild definitions
        if (params.downstreamBuildCfgs != 'empty' && params.downstreamBuildIds != 'empty') {
            List<String> buildIds = params.downstreamBuildIds.split(",").toList()

            if (buildIds.getAt(0) == "THE_END") {
                logger "Downstream Build Chain complete"
            } else {
                // we are currently executing a downstream build which needs to trigger additional downstream build(s)
                logger "Downstream Build Chain detected. Continuing to execute ${params.downstreamBuildIds}"
                List<BanzaiDownstreamBuildCfg> downstreamBuilds  = readJSON(text: params.downstreamBuildCfgs).collect { new BanzaiDownstreamBuildCfg(it) }
                executeBuilds(buildIds, downstreamBuilds)
            }

            return
        }

        // see if we have downstreamBuilds in the cfg
        def buildIds = []
        if (downstreamBuildCfgs.any { it.optional }) {
            logger "Optional Downstream Builds detected"
            buildIds = getBuildIdsWithOptional(cfg, downstreamBuildCfgs)
            if (buildIds.size() == 0) {
                return
            }
        } else {
            buildIds = downstreamBuildCfgs.collect { it.id }
        }

        executeBuilds(buildIds, downstreamBuildCfgs)
    }
}