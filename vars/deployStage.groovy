#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiStepCfg
import com.github.banzaicicd.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.gitOps == null && cfg.deploy == null) { return }

  String stageName = 'Deploy'
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )
  BanzaiStepCfg deployCfg

  banzaiStage.validate {
    if (cfg.gitOps) {
      if (!cfg.internal.gitOps.DEPLOY) {
        // if this is a GitOps repo then cfg.internal.gitOps.DEPLOY must be set
        return "${BRANCH_NAME} does not qualify for GitOps deployment. Skipping ${stageName}"
      }

      deployCfg = new BanzaiStepCfg()
    } else {
      // see if this is a project repo with a deployment configuration
      deployCfg = findValueInRegexObject(cfg.deploy, BRANCH_NAME)

      if (deployCfg == null) {
        logger "returning deploy validation error"
        return "${BRANCH_NAME} does not match a 'deploy' branch pattern. Skipping ${stageName}"
      }
    }
  }

  banzaiStage.execute {
    String script = deployCfg.shell ?: "deploy.sh"
    runScript(cfg, script, cfg.internal.gitOps.DEPLOY_ARGS)
  }
}