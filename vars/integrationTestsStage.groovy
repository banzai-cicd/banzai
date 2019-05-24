#!/usr/bin/env groovy

// named banzaiBuild to avoid collision with existing 'build' jenkins pipeline plugin
def call(config) {
  def stageName = 'IT'
  def stageConfig = getBranchBasedStageConfig(config.integrationTests)
  if (!stageConfig) {
    logger "${BRANCH_NAME} does not match a 'integrationTests' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(config, stageName, 'Pending', 'PENDING', true)

      if (config.xvfb) {
          def screen = config.xvfbScreen ?: '1800x900x24'

          wrap([$class: 'Xvfb', screen: screen]) {
              def script = stageConfig.script ?: "integrationTests.sh"
              runScript(config, script)
          }
      } else {
          def script = stageConfig.script ?: "integrationTests.sh"
          runScript(config, script)
      }

      notify(config, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, stageName, 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }
}
