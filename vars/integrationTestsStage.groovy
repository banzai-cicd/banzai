#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiIntegrationTestsCfg
import com.ge.nola.BanzaiEvent

// named banzaiBuild to avoid collision with existing 'build' jenkins pipeline plugin
def call(BanzaiCfg cfg) {
  if (cfg.integrationTests == null) { return }

  def stageName = 'IT'
  BanzaiIntegrationTestsCfg itCfg = findValueInRegexObject(cfg.integrationTests, BRANCH_NAME)
  if (itCfg == null) {
    logger "${BRANCH_NAME} does not match a 'integrationTests' branch pattern. Skipping ${stageName}"
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

      if (itCfg.xvfb) {
          def screen = itCfg.xvfbScreen ?: '1800x900x24'

          wrap([$class: 'Xvfb', screen: screen]) {
              def script = itCfg.script ?: "integrationTests.sh"
              runScript(cfg, script)
          }
      } else {
          def script = itCfg.script ?: "integrationTests.sh"
          runScript(cfg, script)
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
