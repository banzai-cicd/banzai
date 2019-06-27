#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiStepCfg
import com.github.banzaicicd.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.publish == null) { return }

  String stageName = 'Publish'
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )
  BanzaiStepCfg publishCfg = findValueInRegexObject(cfg.publish, BRANCH_NAME)

  banzaiStage.validate {
    if (publishCfg == null) {
      return "${BRANCH_NAME} does not match a 'publish' branch pattern. Skipping ${stageName}"
    }
  }

  banzaiStage.execute {
    String script = publishCfg.shell ?: "publish.sh"
    runScript(cfg, script)
  }
}
