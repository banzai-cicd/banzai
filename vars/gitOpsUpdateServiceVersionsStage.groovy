#!/usr/bin/env groovy

/**
  Detects if this pipeline was triggered by an upstream job's 'gitOpsTriggerStage' and updates service versions
*/
def call(config) {
  def stageName = 'GitOps: Update Service Versions'

  if (params.gitOpsTriggeringBranch == 'empty' || params.gitOpsVersions == 'empty') {
    logger "Job was not triggered by an upstream service version change. Skipping '${stageName}'"
    return
  }

  stage (stageName) {
    try {
      notify(cfg, [
          scope: BanzaiEvent.Scope.STAGE,
          status: BanzaiEvent.Status.PENDING,
          stage: stageName,
          message: 'Pending'
      ])
      gitOpsUpdateServiceVersions(config)
      notify(cfg, [
          scope: BanzaiEvent.Scope.STAGE,
          status: BanzaiEvent.Status.SUCCESS,
          stage: stageName,
          message: 'Success'
      ])
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notify(cfg, [
            scope: BanzaiEvent.Scope.STAGE,
            status: BanzaiEvent.Status.FAILURE,
            stage: stageName,
            message: 'Failed'
        ])
        error(err.message)
    }
  }
}
