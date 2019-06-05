#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiStepCfg
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg) {
  if (cfg.build == null) { return } 

  String stageName = 'Build'
  BanzaiStepCfg buildCfg = findValueInRegexObject(cfg.build, BRANCH_NAME)

  if (buildCfg == null) {
    logger "${BRANCH_NAME} does not match a 'build' branch pattern. Skipping ${stageName}"
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
      String script = buildCfg.script ?: "build.sh"
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
