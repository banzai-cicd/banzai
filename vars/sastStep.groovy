#!/usr/bin/env groovy

def call(config) {

  if (config.sast) {
    if (config.sastBranches && BRANCH_NAME !=~ config.sastBranches) {
      logger "${BRANCH_NAME} does not match the sastBranches pattern. Skipping"
      return 
    }

    try {
        notify(config, 'SAST', 'Pending', 'PENDING')
        sast(config)
        notify(config, 'SAST', 'Successful', 'SUCCESS')
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'UNSTABLE'
      notify(config, 'Build', 'Failed', 'FAILURE')
      
      error(err.message)
    }
  }

}
