#!/usr/bin/env groovy

def call(config) {

  if (config.promote) {
    try {
      notify(config, 'Promote', 'Pending', 'PENDING', true)
      promote(config)
      notify(config, 'Promote', 'Successful', 'SUCCESS', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, 'Promote', 'Failed', 'FAILURE', true)
      error(err.message)
    }
  }

}
