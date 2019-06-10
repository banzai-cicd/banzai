#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiFilterSecretsCfg
import com.ge.nola.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.filterSecrets == null) { return }

  String stageName = 'Filter Secrets'
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )
  BanzaiFilterSecretsCfg filterSecretsCfg = findValueInRegexObject(cfg.filterSecrets, BRANCH_NAME)

  banzaiStage.validate {
    if (filterSecretsCfg == null) {
      return "${BRANCH_NAME} does not match a 'filterSecrets' branch pattern. Skipping ${stageName}"
    }
  }
  
  banzaiStage.execute {
    filterSecrets(filterSecretsCfg)
  }
}
