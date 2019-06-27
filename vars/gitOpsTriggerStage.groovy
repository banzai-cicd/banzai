#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiGitOpsTriggerCfg
import com.github.banzaicicd.BanzaiEvent

def call(BanzaiCfg cfg) {
  if (cfg.gitOpsTrigger == null) { return }

  def stageName = 'Trigger GitOps'
  BanzaiGitOpsTriggerCfg gitOpsCfg = findValueInRegexObject(cfg.gitOpsTrigger, BRANCH_NAME)
  if (gitOpsCfg == null) {
    logger "${BRANCH_NAME} does not match a 'gitOpsTrigger' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(cfg, [
        scope: BanzaiEvent.Scope.STAGE,
        status: BanzaiEvent.Status.PENDING,
        stage: stageName,
        message: 'Pending'
      ])
      gitOpsTrigger(cfg.userData, gitOpsCfg)
      notify(cfg, [
        scope: BanzaiEvent.Scope.STAGE,
        status: BanzaiEvent.Status.SUCCESS,
        stage: stageName,
        message: 'Success'
      ])
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(cfg, [
        scope: BanzaiEvent.Scope.STAGE,
        status: BanzaiEvent.Status.FAILURE,
        stage: stageName,
        message: 'Failed'
      ])

      error(err.message)
    }
  }
}
