#!/usr/bin/env groovy

def call(config) {
  def stageName = 'PowerDevOps Reporting'

  if (config.powerDevOpsReporting) {    
    logger "powerDevOpsReporting is not present. Skipping ${stageName}"

    if (config.powerDevOpsReporting.branches && BRANCH_NAME !=~ config.powerDevOpsReporting.branches) {
      logger "${BRANCH_NAME} does not match the powerDevOpsReporting.branches pattern. Skipping ${stageName}"
      return 
    }

    stage (stageName) {
      try {
        notify(config, stageName, 'Pending', 'PENDING')
        if (config.httpsProxyHost && config.httpsProxyPort) {
          authenticateService(true)
        }
        reportPipelineStatePublish();
        reportPipelineStateDeploy();
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
