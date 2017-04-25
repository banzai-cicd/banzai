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
          println "${BRANCH_NAME} does not match the publishBranches pattern. Skipping Publish"
          return
        }
      }

      runScript(config, "publishScriptFile", "publishScript")
    }
}
