#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiStepCfg

def call(BanzaiCfg cfg) {
  if (cfg.publish == null) { return }

  String stageName = 'Publish'
  BanzaiStepCfg publishCfg = getBranchBasedConfig(cfg.publish)
  
  if (publishCfg == null) {
    logger "${BRANCH_NAME} does not match a 'publish' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(cfg, stageName, 'Pending', 'PENDING', true)
      String script = publishCfg.script ?: "publish.sh"
      runScript(cfg, script)
      notify(cfg, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        if (isGithubError(err)) {
            notify(cfg, stageName, 'githubdown', 'FAILURE', true)
        } else {
            notify(cfg, stageName, 'Failed', 'FAILURE', true)
        }
        
        error(err.message)
    }
  }
}
