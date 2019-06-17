#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiStageCfg
import com.ge.nola.cfg.BanzaiStepCfg
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg, BanzaiStageCfg stageCfg) {
  String stageName = stageCfg.name
  List<BanzaiStepCfg> stepCfgs = findValueInRegexObject(stageCfg.steps, BRANCH_NAME)

  if (stepCfgs == null) {
    logger "${BRANCH_NAME} does not match a branch pattern for the custom stage '${stageName}'. Skipping ${stageName}"
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

      stepCfgs.each {
          if (it.shell) {
              runScript(cfg, it.shell)
          } else if (it.groovy) {
              it.groovy.call(cfg)
          }
      }
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
