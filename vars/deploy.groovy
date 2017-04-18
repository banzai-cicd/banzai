#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {

  // determine if on develop branch
  def devBranchPattern = config.developBranch
  Pattern devPattern = Pattern.compile(devBranchPattern)
  if ((BRANCH_NAME ==~ devPattern)) {

    sshagent([config.developDeployCredId]) {
       sh "ssh -vvv -o StrictHostKeyChecking=no -l ${config.developDeployUser} ${config.developDeployServer} ${config.developDeployCmd}"
    }

  }

}
