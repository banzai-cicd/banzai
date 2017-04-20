#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {
    def BUILD_SCRIPT_DEFAULT = 'buildScript'

    // now build, based on the configuration provided
    stage ('Build') {
      if (config.buildBranches) {
        def buildBranchesPattern = config.buildBranches
        Pattern pattern = Pattern.compile(buildBranchesPattern)

        if (!(BRANCH_NAME ==~ pattern)) {
          echo "${BRANCH_NAME} does not match the buildBranches pattern. Skipping Build"
          return
        }
      }

      runScript(config, "buildScriptFile", "buildScript", [BRANCH_NAME])
    }
}
