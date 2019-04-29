#!/usr/bin/env groovy

/**
 Stage that can be re-used for vulnerabilityScans and qualityScans
*/
def call(config, type) {
    def scanKey = "${type}Scans"
    def stageName = "${type.capitalize()​} Scans"
    def abortKey = "${type}AbortOnError"

    if (config[scanKey]) {
        // check and see if the current branch matches the config
        def configKey = config[scanKey].keySet().find { BRANCH_NAME ==~ it }
        if (!configKey) {
            logger "${configKey} does not contain an entry that matches the branch: ${BRANCH_NAME}"
            return
        }

        stage (stageName) {
            def scansConfig = config[scanKey][configKey]

            try {
                notify(config, stageName, 'Pending', 'PENDING')
                switch (type) {
                    case 'vulnerability'
                        vulnerabilityScans(config, scansConfig)
                        break
                    case 'quality'
                        qualityScans(config, scansConfig)
                        break
                    default:
                        throw new GroovyRuntimeException("scan with of type '${type}' not recognized")
                }
                notify(config, stageName, 'Successful', 'PENDING')
            } catch (err) {
                echo "Caught: ${err}"
                notify(config, stageName, 'Failed', 'FAILURE')

                // abort if all scans should result in abort OR
                // if this specific scan is configured to abort
                if (config[abortKey] || err.message == "true") {
                    currentBuild.result = 'ABORTED'
                    error(err.message)
                } else {
                    currentBuild.result = 'UNSTABLE'
                }
            }
        }
    }
}
