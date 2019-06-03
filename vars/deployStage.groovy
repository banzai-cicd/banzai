#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Deploy'
  def stageConfig = getBranchBasedConfig(config.deploy)
  if (stageConfig == null) {
    logger "${BRANCH_NAME} does not match a 'deploy' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(config, stageName, 'Pending', 'PENDING', true)
      // TODO: refactor deployArgs
      def script = stageConfig.script ?: "deploy.sh"
      runScript(config, script, config.deployArgs)
      notify(config, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, stageName, 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }

}
