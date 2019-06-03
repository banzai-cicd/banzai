#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Deploy'
  
  if (config.gitOps) {
    if (!config.internal.gitOps.DEPLOY) {
      // if this is a GitOps repo then config.internal.gitOps.DEPLOY must be set
      logger "${BRANCH_NAME} does qualify for GitOps deployment. Skipping ${stageName}"
      return
    }

    stageConfig = [:]
  } else {
    // see if this is a project repo with a deployment configuration
    stageConfig = getBranchBasedConfig(config.deploy)
    
    if (stageConfig == null) {
      logger "${BRANCH_NAME} does not match a 'deploy' branch pattern. Skipping ${stageName}"
      return
    }
  } 

  stage (stageName) {
    try {
      notify(config, stageName, 'Pending', 'PENDING', true)
      // TODO: refactor deployArgs
      def script = stageConfig.script ?: "deploy.sh"
      runScript(config, script, config.internal.gitOps.DEPLOY_ARGS)
      notify(config, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, stageName, 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }

}
