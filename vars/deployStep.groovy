#!/usr/bin/env groovy

def call(config) {

  if (config.deploy) {
    if (config.deployBranches && BRANCH_NAME !=~ config.deployBranches) {
      logger "${BRANCH_NAME} does not match the deployBranchesPattern pattern. Skipping"
      return 
    }

    try {
      notify(config, 'Deploy', 'Pending', 'PENDING', true)
      deploy(config)
      passStep('DEPLOY')
      notify(config, 'Deploy', 'Successful', 'SUCCESS', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, 'Deploy', 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }

}
