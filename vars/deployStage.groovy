#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Deploy'

  if (config.deploy) {
    if (config.deployBranches && !(BRANCH_NAME ==~ config.deployBranches)) {
      logger "${BRANCH_NAME} does not match the deployBranches pattern. Skipping ${stageName}"
      return 
    }

    stage (stageName) {
      try {
        notify(config, stageName, 'Pending', 'PENDING', true)
        deploy(config)
        notify(config, stageName, 'Successful', 'PENDING', true)
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notify(config, stageName, 'Failed', 'FAILURE', true)
        
        error(err.message)
      }
    }
  }

}
