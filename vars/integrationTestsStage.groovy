#!/usr/bin/env groovy

// named banzaiBuild to avoid collision with existing 'build' jenkins pipeline plugin
def call(config) {
  def stageName = 'IT'

  if (config.integrationTests) {
    if (config.integrationTestsBranches && !(BRANCH_NAME ==~ config.integrationTestsBranches)) {
      logger "${BRANCH_NAME} does not match the integrationTestsBranches pattern. Skipping ${stageName}"
      return 
    }

    stage (stageName) {
      try {
        notify(config, stageName, 'Pending', 'PENDING', true)

        if (config.xvfb) {
            def screen = config.xvfbScreen ?: '1800x900x24'

            wrap([$class: 'Xvfb', screen: screen]) {
                integrationTests(config)
            }
        } else {
            integrationTests(config)
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
}
