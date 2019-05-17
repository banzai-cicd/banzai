#!/usr/bin/env groovy

/**
  Detects if this pipeline was triggered by an upstream job's 'gitOpsTriggerStage' and updates service versions
*/
def call(config) {
  def stageName = 'GitOps Update Service Versions'

  if (params.gitOpsTriggeringBranch == 'empty' || params.gitOpsVersions == 'empty') {
    logger "Job was not triggered by an upstream service version change. Skipping '${stageName}'"
    return
  }

  stage (stageName) {
    try {
      notify(config, stageName, 'Pending', 'PENDING', true)
      gitOpsUpdateServiceVersions(config)
      notify(config, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notify(config, stageName, 'Failed', 'FAILURE', true)
        error(err.message)
    }
  }
}
