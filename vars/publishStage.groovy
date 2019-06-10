#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiStepCfg
import com.ge.nola.BanzaiStage

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
    String script = publishCfg.script ?: "publish.sh"
    runScript(cfg, script)
  }
}
