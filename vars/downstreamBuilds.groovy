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

def call(config) {
    stage ('Downstream Builds') {

        if (config.downstreamBuildBranches) {
            Pattern pattern = Pattern.compile(config.downstreamBuildBranches)

            if (!(BRANCH_NAME ==~ pattern)) {
                logger "${BRANCH_NAME} does not match the downstreamBuildBranches pattern. Skipping Downstream Builds"
                return
            }
        }

        withCredentials([string(credentialsId: config.gitTokenId, variable: 'TOKEN')]) {
            def url = scm.getUserRemoteConfigs()[0].getUrl()
            def orgName = determineOrgName(url)
            def repoName = determineRepoName(url)

            def branchInfoUrl = "https://github.build.ge.com/api/v3/repos/${orgName}/${repoName}/branches/${BRANCH_NAME}"
            def branchInfoResponse = httpRequest(url: branchInfoUrl, customHeaders: [[Authorization: "token ${TOKEN}"]])
            Map branchInfo = (Map) new JsonSlurper().parseText(response.content)
            def latestCommit = branchInfo.commit.sha
            logger "latest commit: ${latestCommit}"
        }
        
        // getLatestCommitHash  commit.sha
        // lookupClosedPrWithMatchingMergeHash https://github.build.ge.com/api/v3/repos/PowerApi/power-api/pulls?state=closed [*].merge_commit_sha
        // checkForDownstreamBuildLabels result.labels
    }
}