#!/usr/bin/env groovy

def call(config) {

  if (config.build) {
    if (config.buildBranches && (!(BRANCH_NAME ==~ config.buildBranches))) {
      logger "${BRANCH_NAME} does not match the buildBranches pattern. Skipping"
      return 
    }


    try {
        notify(config, 'Build', 'Pending', 'PENDING')
        banzaiBuild(config)
        notify(config, 'Build', 'Successful', 'PENDING')
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        if (isGithubError(err)) {
            notify(config, 'Build', 'githubdown', 'FAILURE', true)
        } else {
            notify(config, 'Build', 'Failed', 'FAILURE')
        }
        
        error(err.message)
    }
  }

}
