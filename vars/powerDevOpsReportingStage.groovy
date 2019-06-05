#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg) {
  def stageName = 'PowerDevOps Reporting'

  if (cfg.powerDevOpsReporting) {    
    if (cfg.powerDevOpsReporting.branches && !(BRANCH_NAME ==~ cfg.powerDevOpsReporting.branches)) {
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
        if (cfg.httpsProxy) {
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
