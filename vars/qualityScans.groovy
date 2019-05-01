#!/usr/bin/env groovy
import main.groovy.cicd.pipeline.settings.PipelineSettings;

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

                            def proxyOn = false
                            def sonarHost = PipelineSettings.SonarQubeSettings.sonarHostUrl.replaceFirst(/(http|https):\/\//, "")
                            logger "config.noProxy contains ${sonarHost}? ${config.noProxy.contains(sonarHost)}"
                            if (!config.noProxy || !config.noProxy.contains(sonarHost)) {
                                if (config.proxyHost && config.proxyPort) {
                                    logger "setting sonar proxy ${config.proxyHost}:${config.proxyPort}"
                                    proxyOn = true
                                }
                            }

                            powerDevOpsSonarGate(proxyOn);

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