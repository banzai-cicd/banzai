#!/usr/bin/env groovy

def call(config, scansConfig) {
    def stages = [:]
    
    scansConfig.each {
        switch (it.type) {
            case "sonar": // requires https://github.build.ge.com/PowerDevOps/jenkins-master-shared-library
                stages[it.type] = {
                    stage("Sonar") {
                        try {
                            notify(config, 'Sonar', 'Pending', 'PENDING')
                            sonarqubeQualityCheck();
                            powerDevOpsSonarGate();
                            notify(config, 'Sonar', 'Successful', 'PENDING')
                        } catch (err) {
                            echo "Caught: ${err}"
                            currentBuild.result = 'UNSTABLE'
                            notify(config, 'Sonar', 'Failed', 'FAILURE')
                            def abort = it.abortOnError ? "true" : "false"
                            error(abort) // let the scansStage know if it should abort
                        }
                    }
                }
                break
            default:
                logger("Unable to parse qualityScans config item: ${it}")
                break
        }
    }

    parallel(stages)
}