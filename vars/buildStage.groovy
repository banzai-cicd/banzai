#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Build'
  def stageConfig = getBranchBasedStageConfig(config.build)
  if (!stageConfig) {
    logger "${BRANCH_NAME} does not match a 'build' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(config, stageName, 'Pending', 'PENDING')
      def script = stageConfig.script ?: "build.sh"
      runScript(config, script)
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
