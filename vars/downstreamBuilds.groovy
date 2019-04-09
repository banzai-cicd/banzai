#!/usr/bin/env groovy

import java.util.regex.Pattern
import java.util.regex.Matcher
import groovy.json.JsonSlurper


String determineOrgName(url) {
    def finder = (url =~ /:([^:]*)\//)

    return finder.getAt(0).getAt(1)
}

String determineRepoName(url) {
    return scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
}

def executeBuild(downstreamBuilds, labels) {
    def targetLabel = labels.removeAt(0)
    def buildKey = targetLabel.replace("build:", "")​​​​​​​​​​​​​​​​​​​​​​​​​​​​​​

    // execute downstream build and pass on remaining build list and downstreamBuilds map
    build(job: downstreamBuilds[buildKey],
          parameters: [
              [$class: 'StringParameterValue', name: 'downstreamBuildList', value: labels.join(',')],
              [$class: 'StringParameterValue', name: 'downstreamBuilds', value: JsonOutput.toJson(downstreamBuilds)]
            ]
        )
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

        if (binding.hasVariable('downstreamBuildList') && downstreamBuildList.split(",").size > 0) {
            // we are currently executing a downstream build which needs to trigger additional downstream build(s)
            logger "downstreamBuildList detected. executing"
            def downstreamBuildsParsed = readJSON(text: downstreamBuilds)
            executeBuild(downstreamBuildsParsed, downstreamBuildList.split(","))
            return
        }

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
            logger "Latest Commit: ${latestCommit}"

            // find the pr with this merge_hash
            def prListUrl = "https://github.build.ge.com/api/v3/repos/${orgName}/${repoName}/pulls?state=closed"
            def prListResponse = httpRequest(url: prListUrl, customHeaders: [[maskValue: false, name: 'Authorization', value: "token ${TOKEN}"]])
            def prList = readJSON(text: prListResponse.content)
            def targetPr = prList.find { it.merge_commit_sha == latestCommit }
            logger "Associated PR found. PR:${targetPr.number}"
            
            // determine if it has any labels
            if (targetPr.labels.size > 0) {
                logger "Labels detected: ${targetPr.labels.join(',')}"
                def labelValues = []
                targetPr.labels.each { labelValues.add(it.name) }

                executeBuild(config.downstreamBuilds, labelValues)
            } else {
                logger "No labels found for the associated pr. Will not trigger downstream builds"
            }
        }
        
        // getLatestCommitHash  commit.sha
        // lookupClosedPrWithMatchingMergeHash https://github.build.ge.com/api/v3/repos/PowerApi/power-api/pulls?state=closed [*].merge_commit_sha
        // checkForDownstreamBuildLabels result.labels
    }
}