#!/usr/bin/env groovy


def call(config) {
  def stageName = 'GitOps: User Input Stages'
  if (!config.gitOps || params.gitOpsTriggeringBranch != 'empty') {
      logger "Does not appear to be a user-initiated GitOps build. Skipping '${stageName}'"
      return
  }

  // prompt the user to determine which type of deployment they would like to achieve.
  // we will support 2 types first. 'version-selection' and 'environment promotion'
  
  stage ('Select Deployment Type') {
    def deploymentType
    timeout(time: 10, unit: 'MINUTES') {
      script {
        deploymentType = input(
          id: 'deploymentTypeInput', 
          message: 'What type of deployment?',
          ok: 'Next Step',
          parameters: [choice(name: 'deploymentType', choices: 'Version Selection\nEnvironment Promotion', description: 'What is the release scope?')]
        )
      }

      logger "Choice selcted! ${deploymentType}"
    }
  }
}
