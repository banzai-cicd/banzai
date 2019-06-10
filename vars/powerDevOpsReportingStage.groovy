#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.powerDevOpsReporting == null) { return }

  def stageName = 'PowerDevOps Reporting'
  BanzaiBaseStage banzaiStage = new BanzaiBaseStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )

  banzaiStage.validate {
    if (cfg.powerDevOpsReporting.branches && !(BRANCH_NAME ==~ cfg.powerDevOpsReporting.branches)) {
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
