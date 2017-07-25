#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {
    stage ('Publish') {

      if (config.publishBranches) {
        Pattern pattern = Pattern.compile(config.publishBranches)

        if (!(BRANCH_NAME ==~ pattern)) {
          println "${BRANCH_NAME} does not match the publishBranches pattern. Skipping Publish"
          return
        }
      }

      runScript(config, "publishScriptFile", "publishScript")
    }
}
