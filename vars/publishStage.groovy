#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiStepCfg
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg) {
  if (cfg.publish == null) { return }

  String stageName = 'Publish'
  BanzaiStepCfg publishCfg = findValueInRegexObject(cfg.publish, BRANCH_NAME)
  
  if (publishCfg == null) {
    logger "${BRANCH_NAME} does not match a 'publish' branch pattern. Skipping ${stageName}"
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
      String script = publishCfg.script ?: "publish.sh"
      runScript(cfg, script)
      notify(cfg, [
        scope: BanzaiEvent.Scope.STAGE,
        status: BanzaiEvent.Status.SUCCESS,
        stage: stageName,
        message: 'Success'
      ])
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        if (isGithubError(err)) {
          notify(cfg, [
            scope: BanzaiEvent.Scope.STAGE,
            status: BanzaiEvent.Status.FAILURE,
            stage: stageName,
            message: 'githubdown'
          ])
        } else {
          notify(cfg, [
            scope: BanzaiEvent.Scope.STAGE,
            status: BanzaiEvent.Status.FAILURE,
            stage: stageName,
            message: 'Failed'
          ])   
        }
        
        error(err.message)
    }
  }
}
