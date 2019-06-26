#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiStage

def call(BanzaiCfg cfg, Map powerDevOpsReporting) {
  if (powerDevOpsReporting == null) { return }

  def stageName = 'PowerDevOps Reporting'
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )

  banzaiStage.validate {
    if (powerDevOpsReporting.branches && !(BRANCH_NAME ==~ powerDevOpsReporting.branches)) {
      return "${BRANCH_NAME} does not match the powerDevOpsReporting.branches pattern. Skipping ${stageName}"
    }
  }

  banzaiStage.execute {
    if (cfg.httpsProxy) {
      authenticateService(true)
    }
    reportPipelineStatePublish();
    reportPipelineStateDeploy();
  }
}
