#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg

/**
 Stage that can be re-used for vulnerabilityScans and qualityScans
*/
def call(BanzaiCfg cfg, String type) {
    def scanKey = "${type}Scans"
    def stageName = "${type.substring(0, 1).toUpperCase() + type.substring(1)} Scans"
    def abortKey = "${type}AbortOnError"

    if (cfg[scanKey]) {
        // check and see if the current branch matches the cfg
        def scansConfig = getBranchBasedConfig(cfg[scanKey])
        if (scansConfig == null) {
            logger "${BRANCH_NAME} does match a '${scanKey}' branch pattern. Skipping ${stageName}"
            return
        }

        stage (stageName) {
            try {
                notify(cfg, stageName, 'Pending', 'PENDING')
                switch (type) {
                    case 'vulnerability':
                        vulnerabilityScans(cfg, scansConfig)
                        break
                    case 'quality':
                        qualityScans(cfg, scansConfig)
                        break
                    default:
                        throw new GroovyRuntimeException("scan with of type '${type}' not recognized")
                }
                notify(cfg, stageName, 'Successful', 'PENDING')
            } catch (err) {
                echo "Caught: ${err}"
                notify(cfg, stageName, 'Failed', 'FAILURE')

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
}
