#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Publish'

  if (config.publish) {
    if (config.publishBranches && !(BRANCH_NAME ==~ config.publishBranches)) {
      logger "${BRANCH_NAME} does not match the publishBranches pattern. Skipping ${stageName}"
      return 
    }

    stage (stageName) {
      try {
        notify(config, stageName, 'Pending', 'PENDING', true)
        publish(config)
        notify(config, stageName, 'Successful', 'PENDING', true)
      } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          if (isGithubError(err)) {
              notify(config, stageName, 'githubdown', 'FAILURE', true)
          } else {
              notify(config, stageName, 'Failed', 'FAILURE', true)
          }
          
          error(err.message)
      }
    }
  }
}
