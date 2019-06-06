#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiFilterSecretsCfg
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg) {
  if (cfg.filterSecrets == null) { return }

  def stageName = 'Filter Secrets'
  BanzaiFilterSecretsCfg filterSecretsCfg = findValueInRegexObject(cfg.filterSecrets, BRANCH_NAME)
  if (filterSecretsCfg == null) {
    logger "${BRANCH_NAME} does not match a 'filterSecrets' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    notify(cfg, [
        scope: BanzaiEvent.Scope.STAGE,
        status: BanzaiEvent.Status.PENDING,
        stage: stageName,
        message: 'Pending'
    ])
    filterSecrets(filterSecretsCfg)
    notify(cfg, [
        scope: BanzaiEvent.Scope.STAGE,
        status: BanzaiEvent.Status.SUCCESS,
        stage: stageName,
        message: 'Success'
    ])
  }
}
