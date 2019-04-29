#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Mark For Promotion'

  if (config.markForPromotion) {
    if (config.markForPromotion && BRANCH_NAME !=~ config.markForPromotion) {
      logger "${BRANCH_NAME} does not match the markForPromotion pattern. Skipping ${stageName}"
      return 
    }

    stage (stageName) {
		  try {
        notify(config, stageName, 'Pending', 'PENDING', true)
        markForPromotion(config)
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
