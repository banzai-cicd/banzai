#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {

  stage ('Deploy') {

    if (config.deployBranches) {
      def deployBranchesPattern = config.deployBranches
      Pattern pattern = Pattern.compile(deployBranchesPattern)

      if ((BRANCH_NAME ==~ deployBranchesPattern)) {
        if (config.deployCmd) {
          if (!config.deployUser) {
            println "Deploy: No deployUser specified! Skipping Deploy"
            return
          }
          if (!config.deployServer) {
            println "Deploy: No deployServer specified! Skipping Deploy"
            return
          }
          sshagent([config.deployCredId]) {
             sh "ssh -o StrictHostKeyChecking=no ${config.developDeployUser}@${config.developDeployServer} '${config.developDeployCmd}'"
          }
        } else {
          sshagent([config.deployCredId]) {
            runScript(config, "deployScriptFile", "deployScript")
          }
        }
      }
    } else if (config.developBranch) {
      def devBranchPattern = config.developBranch
      Pattern devPattern = Pattern.compile(devBranchPattern)

      if ((BRANCH_NAME ==~ devPattern)) {
        if (config.developDeployCmd) {
          sshagent([config.developDeployCredId]) {
             sh "ssh -o StrictHostKeyChecking=no ${config.developDeployUser}@${config.developDeployServer} '${config.developDeployCmd}'"
          }
        }
      }
    }
  }

}
