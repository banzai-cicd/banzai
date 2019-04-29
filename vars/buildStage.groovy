#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Build'
  
  if (config.build) {
    if (config.buildBranches && BRANCH_NAME !=~ config.buildBranches) {
      logger "${BRANCH_NAME} does not match the buildBranches pattern. Skipping ${stageName}"
      return 
    }

    stage (stageName) {
      try {
        notify(config, stageName, 'Pending', 'PENDING')
        banzaiBuild(config)
        notify(config, stageName, 'Successful', 'PENDING')
      } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          if (isGithubError(err)) {
              notify(config, stageName, 'githubdown', 'FAILURE', true)
          } else {
              notify(config, stageName, 'Failed', 'FAILURE')
          }
          
          error(err.message)
      }
    }
    
  }

}
