#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {
    stage ('IT') {

      if (config.integrationTestsBranches) {
        Pattern pattern = Pattern.compile(config.integrationTestsBranches)

        if (!(BRANCH_NAME ==~ pattern)) {
          println "${BRANCH_NAME} does not match the integrationTestsBranches pattern. Skipping IT"
          return
        }
      }

      runScript(config, "integrationTestsScriptFile", "integrationTestsScript", [BRANCH_NAME])
    }
}
