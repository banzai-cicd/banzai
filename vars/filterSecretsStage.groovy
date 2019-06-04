#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiFilterSecretsCfg

def call(BanzaiCfg cfg) {
  if (cfg.filterSecrets == null) { return }

  def stageName = 'Filter Secrets'
  BanzaiFilterSecretsCfg filterSecretsCfg = getBranchBasedConfig(cfg.filterSecrets)
  if (filterSecretsCfg == null) {
    logger "${BRANCH_NAME} does not match a 'filterSecrets' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    notify(cfg, stageName, 'Pending', 'PENDING')
    filterSecrets(filterSecretsCfg)
    notify(cfg, stageName, 'Successful', 'PENDING')
  }
}
