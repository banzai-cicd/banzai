#!/usr/bin/env groovy

def call(config) {

  if (config.markForPromotion) {
    if (!(BRANCH_NAME ==~ config.markForPromotion)) {
      logger "${BRANCH_NAME} does not match the markForPromotion pattern. Skipping"
      return 
    }

    stage ('Mark For Promotion') {
		  try {
        notify(config, 'MarkForPromotion', 'Pending', 'PENDING', true)
        markForPromotion(config)
        notify(config, 'MarkForPromotion', 'Successful', 'PENDING', true)
      } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          notify(config, 'MarkForPromotion', 'Failed', 'FAILURE', true)
          error(err.message)
      }
	  }
  }
}
