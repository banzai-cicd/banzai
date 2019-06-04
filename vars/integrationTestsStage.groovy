#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiIntegrationTestsCfg

// named banzaiBuild to avoid collision with existing 'build' jenkins pipeline plugin
def call(BanzaiCfg cfg) {
  if (cfg.integrationTests == null) { return }

  def stageName = 'IT'
  BanzaiIntegrationTestsCfg itCfg = getBranchBasedConfig(cfg.integrationTests)
  if (itCfg == null) {
    logger "${BRANCH_NAME} does not match a 'integrationTests' branch pattern. Skipping ${stageName}"
    return
  }

  stage (stageName) {
    try {
      notify(cfg, stageName, 'Pending', 'PENDING', true)

      if (cfg.xvfb) {
          def screen = itCfg.xvfbScreen ?: '1800x900x24'

          wrap([$class: 'Xvfb', screen: screen]) {
              def script = itCfg.script ?: "integrationTests.sh"
              runScript(cfg, script)
          }
      } else {
          def script = itCfg.script ?: "integrationTests.sh"
          runScript(cfg, script)
      }

      notify(cfg, stageName, 'Successful', 'PENDING', true)
    } catch (err) {
      echo "Caught: ${err}"
      currentBuild.result = 'FAILURE'
      notify(cfg, stageName, 'Failed', 'FAILURE', true)
      
      error(err.message)
    }
  }
}
