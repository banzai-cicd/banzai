#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {

  stage ('Deploy') {

    if (config.deployBranches) {
      def deployBranchesPattern = config.deployBranches
      Pattern pattern = Pattern.compile(deployBranchesPattern)

      if (!(BRANCH_NAME ==~ pattern)) {
        println "${BRANCH_NAME} does not match the deployBranchesPattern pattern. Skipping Deploy"
        return
      }
    }

    runDeploy(config)
  }

}
