#!/usr/bin/env groovy
import com.ge.nola.banzai.cfg.BanzaiCfg
import com.ge.nola.banzai.cfg.BanzaiVulnerabilityCfg
import com.ge.nola.banzai.cfg.BanzaiQualityCfg
import com.ge.nola.banzai.BanzaiStage

/*
 Stage that can be re-used for vulnerabilityScans and qualityScans
*/
def call(BanzaiCfg cfg, String type) {
  String scanKey = "${type}Scans"
  if (!cfg[scanKey]) { return }

  String stageName = "${type.substring(0, 1).toUpperCase() + type.substring(1)} Scans"
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )
  String abortKey = "${type}AbortOnError"
  // check and see if the current branch matches the cfg
  def scanCfgs = findValueInRegexObject(cfg[scanKey], BRANCH_NAME)

  banzaiStage.validate {
    if (scanCfgs == null) {
      return "${BRANCH_NAME} does match a '${scanKey}' branch pattern. Skipping ${stageName}"
    }
  }

  banzaiStage.execute {
    try {
      switch (type) {
        case 'vulnerability':
          vulnerabilityScans(cfg, (List<BanzaiVulnerabilityCfg>) scanCfgs)
          break
        case 'quality':
          qualityScans(cfg, (List<BanzaiQualityCfg>) scanCfgs)
          break
        default:
          throw new GroovyRuntimeException("scan with of type '${type}' not recognized")
      }
    } catch (Exception e) {
      logger "${e.message}"
      // abort if all scans should result in abort OR
      // if this specific scan is configured to abort
      if (cfg[abortKey] || e.message == 'true') {
        currentBuild.result = 'ABORTED'
        throw new Exception(e.message)
      } else {
        currentBuild.result = 'UNSTABLE'
      }
    }
  }
}
