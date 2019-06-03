#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Publish'
  def stageConfig = getBranchBasedConfig(config.publish)
  if (stageConfig == null) {
    logger "${BRANCH_NAME} does not match a 'publish' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(config, stageName, 'Pending', 'PENDING', true)
      def script = stageConfig.publish ?: "publish.sh"
      runScript(config, script)
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
