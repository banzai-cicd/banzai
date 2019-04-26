#!/usr/bin/env groovy

def call(config) {

  if (!config.skipSCM) {
    try {
        notify(config, 'Checkout', 'Pending', 'PENDING')
        checkout scm
        notify(config, 'Checkout', 'Successful', 'PENDING')
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'UNSTABLE'
        if (isGithubError(err)) {
            notify(config, 'Checkout', 'githubdown', 'FAILURE', true)
        } else {
            notify(config, 'Checkout', 'Failed', 'FAILURE')
        }
        error(err.message)
    }
  }

}
