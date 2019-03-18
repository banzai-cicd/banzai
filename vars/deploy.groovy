#!/usr/bin/env groovy

import java.util.regex.Pattern

def call(config) {
  stage ('Deploy') {

    if (config.deployBranches) {
      Pattern pattern = Pattern.compile(config.deployBranches)

      if (!(BRANCH_NAME ==~ pattern)) {
        logger "${BRANCH_NAME} does not match the deployBranchesPattern pattern. Skipping Deploy"
        return
      }
    }

    runDeploy(config)
  }

}
