#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiStepCfg
import com.github.banzaicicd.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.build == null) { return }

  String stageName = 'Build'
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )
  BanzaiStepCfg buildCfg = findValueInRegexObject(cfg.build, BRANCH_NAME)
  
  banzaiStage.validate {
    if (buildCfg == null) {
      return "${BRANCH_NAME} does not match a 'build' branch pattern. Skipping ${stageName}"
    }
  }

  banzaiStage.execute {
    String script = buildCfg.shell ?: "build.sh"
    runScript(cfg, script)
  }
}