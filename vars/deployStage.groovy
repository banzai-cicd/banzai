#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiUserCfg

def call(BanzaiCfg cfg) {
  String stageName = 'Deploy'
  BanzaiUserCfg deployCfg

  if (cfg.gitOps) {
    if (!cfg.internal.gitOps.DEPLOY) {
      // if this is a GitOps repo then cfg.internal.gitOps.DEPLOY must be set
      logger "${BRANCH_NAME} does qualify for GitOps deployment. Skipping ${stageName}"
      return
    }

    deployCfg = new BanzaiUserCfg()
  } else {
    if (cfg.deploy == null) { return }

    // see if this is a project repo with a deployment configuration
    deployCfg = getBranchBasedConfig(cfg.deploy)
    
    if (deployCfg == null) {
      logger "${BRANCH_NAME} does not match a 'deploy' branch pattern. Skipping ${stageName}"
      return
    }
  } 

  stage (stageName) {
    try {
      notify(cfg, stageName, 'Pending', 'PENDING', true)
      // TODO: refactor deployArgs
      String script = deployCfg.script ?: "deploy.sh"
      runScript(cfg, script, cfg.internal.gitOps.DEPLOY_ARGS)
      notify(cfg, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(cfg, stageName, 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }

}
