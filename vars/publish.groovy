#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {

    // now build, based on the configuration provided
    stage ('Publish') {
      if (config.publishBranches) {
        def publishBranchesPattern = config.publishBranches
        Pattern pattern = Pattern.compile(publishBranchesPattern)

        if (!(BRANCH_NAME ==~ pattern)) {
          echo "${BRANCH_NAME} does not match the publishBranches pattern. Skipping Publish"
          return
        }
      }

      // determine if we should push a 'latest' tag based on the current branch
      // may remove this later and just let this happen in the .sh
      def devBranchPattern = config.developBranch
      Pattern devPattern = Pattern.compile(devBranchPattern)
      def publishLatestTag = false
      if ((BRANCH_NAME ==~ devPattern)) {
        publishLatestTag = true
      }

      def scriptArgs = [DOCKER_REPO_URL, config.appName, publishLatestTag]
      runScript(config, "publishScriptFile", "publishScript", scriptArgs)
    }
}
