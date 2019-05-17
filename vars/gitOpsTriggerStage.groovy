#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Trigger GitOps'

  if (config.gitOpsTrigger && !(BRANCH_NAME ==~ config.gitOpsTrigger.branches)) {
      logger "${BRANCH_NAME} does not match the gitOpsTrigger.branches pattern. Skipping"
      return 
  }

  stage (stageName) {
    try {
      notify(config, stageName, 'Pending', 'PENDING', true)
      gitOpsTrigger(config)
      notify(config, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(config, stageName, 'Failed', 'FAILURE', true)
      error(err.message)
    }
  }
}
