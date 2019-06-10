#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiStepCfg
import com.ge.nola.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.build == null) { return }

  String stageName = 'Build'
  BanzaiBaseStage banzaiStage = new BanzaiBaseStage(
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
    String script = buildCfg.script ?: "build.sh"
    runScript(cfg, script)
  }
}