#!/usr/bin/env groovy

def call(config) {
  def stageName = 'PowerDevOps Reporting'

  if (config.powerDevOpsReporting) {    
    if (config.powerDevOpsReporting.branches && !(BRANCH_NAME ==~ config.powerDevOpsReporting.branches)) {
      logger "${BRANCH_NAME} does not match the powerDevOpsReporting.branches pattern. Skipping ${stageName}"
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
        if (config.httpsProxy) {
          authenticateService(true)
        }
        reportPipelineStatePublish();
        reportPipelineStateDeploy();
        notify(cfg, [
            scope: BanzaiEvent.Scope.STAGE,
            status: BanzaiEvent.Status.SUCCESS,
            stage: stageName,
            message: 'Success'
        ])
      } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          if (isGithubError(err)) {
            notify(cfg, [
              scope: BanzaiEvent.Scope.STAGE,
              status: BanzaiEvent.Status.FAILURE,
              stage: stageName,
              message: 'githubdown'
            ])
          } else {
            notify(cfg, [
              scope: BanzaiEvent.Scope.STAGE,
              status: BanzaiEvent.Status.FAILURE,
              stage: stageName,
              message: 'Failed'
            ])   
          }
          
          error(err.message)
      }
    }
  }
}
