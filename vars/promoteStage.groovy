#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Promote'

  if (config.promote) {
    try {
      notify(config, stageName, 'Pending', 'PENDING', true)
      promote(config)
      notify(config, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, stageName, 'Failed', 'FAILURE', true)
      error(err.message)
    }
  }

}
