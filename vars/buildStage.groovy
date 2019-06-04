#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiUserStepCfg

def call(BanzaiCfg cfg) {
  if (cfg.build == null) { return } 

  String stageName = 'Build'
  BanzaiUserStepCfg buildCfg = getBranchBasedConfig(cfg.build)

  if (buildCfg == null) {
    logger "${BRANCH_NAME} does not match a 'build' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(cfg, stageName, 'Pending', 'PENDING')
      String script = buildCfg.script ?: "build.sh"
      runScript(cfg, script)
      notify(cfg, stageName, 'Successful', 'PENDING')
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        if (isGithubError(err)) {
            notify(cfg, stageName, 'githubdown', 'FAILURE', true)
        } else {
            notify(cfg, stageName, 'Failed', 'FAILURE')
        }
        
        error(err.message)
    }
  }

}
