#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiStageCfg
import com.ge.nola.BanzaiStepCfg

def call(BanzaiCfg cfg, BanzaiStageCfg stageCfg) {
  String stageName = stageCfg.name
  List<BanzaiStepCfg> stepsCfg = getBranchBasedConfig(stageCfg.steps)

  if (stepsCfg == null) {
    logger "${BRANCH_NAME} does not match a branch pattern for the custom stage '${stageName}'. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(cfg, stageName, 'Pending', 'PENDING')
      steps.each {
          if (it.script) {
              runScript(cfg, it.script)
          } else if (it.closure) {
              it.closure.call(cfg)
          }
      }
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
