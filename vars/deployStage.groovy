#!/usr/bin/env groovy

def call(config) {

  if (config.deploy) {

    if (config.deployBranches && !(BRANCH_NAME ==~ config.deployBranches)) {
      logger "${BRANCH_NAME} does not match the deployBranches pattern. Skipping"
      return 
    }

    try {
      notify(config, 'Deploy', 'Pending', 'PENDING', true)
      deploy(config)
      notify(config, 'Deploy', 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, 'Deploy', 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }

}
