#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {
    def BUILD_SCRIPT_DEFAULT = 'itScript'

    // now build, based on the configuration provided
    stage ('IT') {

      if (config.itBranches) {
        Pattern pattern = Pattern.compile(config.itBranches)

        if (!(BRANCH_NAME ==~ pattern)) {
          println "${BRANCH_NAME} does not match the itBranches pattern. Skipping IT"
          return
        }
      }

      runScript(config, "itScriptFile", "itScript", [BRANCH_NAME])
    }
}
