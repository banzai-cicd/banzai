#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

// named banzaiBuild to avoid collision with existing 'build' jenkins pipeline plugin
def call(config) {

  if (config.integrationTests) {
    if (config.integrationTestsBranches && BRANCH_NAME !=~ config.integrationTestsBranches) {
      logger "${BRANCH_NAME} does not match the integrationTestsBranches pattern. Skipping"
      return 
    }

    try {
      notify(config, 'IT', 'Pending', 'PENDING', true)

      if (config.xvfb) {
          def screen = config.xvfbScreen ?: '1800x900x24'

          wrap([$class: 'Xvfb', screen: screen]) {
              integrationTests(config)
          }
      } else {
          integrationTests(config)
      }

      notify(config, 'IT', 'Successful', 'SUCCESS', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, 'IT', 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }

}
