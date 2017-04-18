#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {

  stage ('Deploy') {
    // determine if on develop branch
    def devBranchPattern = config.developBranch
    Pattern devPattern = Pattern.compile(devBranchPattern)
    if ((BRANCH_NAME ==~ devPattern)) {

      sshagent([config.developDeployCredId]) {
         sh "ssh -o StrictHostKeyChecking=no ${config.developDeployUser}@${config.developDeployServer} '${config.developDeployCmd}'"
      }

    }
  }

}
