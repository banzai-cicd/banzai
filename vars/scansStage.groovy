#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiVulnerabilityCfg
import com.ge.nola.BanzaiQualityCfg
import com.ge.nola.BanzaiEvent

/**
 Stage that can be re-used for vulnerabilityScans and qualityScans
*/
def call(BanzaiCfg cfg, String type) {
    def scanKey = "${type}Scans"
    if (!cfg[scanKey]) { return }

    def stageName = "${type.substring(0, 1).toUpperCase() + type.substring(1)} Scans"
    def abortKey = "${type}AbortOnError"
    // check and see if the current branch matches the cfg
    def scanCfgs = findValueInRegexObject(cfg[scanKey], BRANCH_NAME)
    if (scanCfgs == null) {
        logger "${BRANCH_NAME} does match a '${scanKey}' branch pattern. Skipping ${stageName}"
        return
    }

    stage (stageName) {
        try {
            notify(cfg, [
                scope: BanzaiEvent.Scope.STAGE,
                status: BanzaiEvent.Status.PENDING,
                stage: this.stageName,
                message: 'Pending'
            ])
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
            notify(cfg, [
                scope: BanzaiEvent.Scope.STAGE,
                status: BanzaiEvent.Status.SUCCESS,
                stage: this.stageName,
                message: 'Success'
            ])
        } catch (err) {
            echo "Caught: ${err}"
            notify(cfg, [
                scope: BanzaiEvent.Scope.STAGE,
                status: BanzaiEvent.Status.FAILURE,
                stage: this.stageName,
                message: 'Failed'
            ]) 

            // abort if all scans should result in abort OR
            // if this specific scan is configured to abort
            if (cfg[abortKey] || err.message == "true") {
                currentBuild.result = 'ABORTED'
                error(err.message)
            } else {
                currentBuild.result = 'UNSTABLE'
            }
        }
    }
}
