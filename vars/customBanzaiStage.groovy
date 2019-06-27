#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiStageCfg
import com.github.banzaicicd.cfg.BanzaiStepCfg
import com.github.banzaicicd.BanzaiStage

def call(BanzaiCfg cfg, BanzaiStageCfg stageCfg) {
  String stageName = stageCfg.name
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )
  List<BanzaiStepCfg> stepCfgs = findValueInRegexObject(stageCfg.steps, BRANCH_NAME)

  banzaiStage.validate {
    if (stepCfgs == null) {
      return "${BRANCH_NAME} does not match a branch pattern for the custom stage '${stageName}'. Skipping ${stageName}"
    }
  }
  if (stepCfgs == null) {
    logger "${BRANCH_NAME} does not match a branch pattern for the custom stage '${stageName}'. Skipping ${stageName}"
    return
  }


  banzaiStage.execute {
    stepCfgs.each {
        if (it.shell) {
            runScript(cfg, it.shell)
        } else if (it.groovy) {
            it.groovy.call(cfg)
        }
    }
  }
}
