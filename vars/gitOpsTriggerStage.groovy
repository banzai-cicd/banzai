#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiGitOpsTriggerCfg

def call(BanzaiCfg cfg) {
  if (cfg.gitOpsTrigger == null) { return }

  def stageName = 'Trigger GitOps'
  BanzaiGitOpsTriggerCfg gitOpsCfg = getBranchBasedConfig(cfg.gitOpsTrigger)
  if (gitOpsCfg == null) {
    logger "${BRANCH_NAME} does not match a 'gitOpsTrigger' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(cfg, stageName, 'Pending', 'PENDING', true)
      gitOpsTrigger(gitOpsCfg)
      notify(cfg, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(cfg, stageName, 'Failed', 'FAILURE', true)
      error(err.message)
    }
  }
}
