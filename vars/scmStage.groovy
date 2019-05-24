#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Checkout'

  if (!config.skipSCM) {
    try {
        notify(config, stageName, 'Pending', 'PENDING')
        checkout([
          $class: 'GitSCM',
          branches: scm.branches,
          doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
          extensions: scm.extensions + [$class: 'LocalBranch', localBranch: "**"],
          userRemoteConfigs: scm.userRemoteConfigs
        ])
        notify(config, stageName, 'Successful', 'PENDING')
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'UNSTABLE'
        if (isGithubError(err)) {
            notify(config, stageName, 'githubdown', 'FAILURE', true)
        } else {
            notify(config, stageName, 'Failed', 'FAILURE')
        }
        error(err.message)
    }
  }

}
