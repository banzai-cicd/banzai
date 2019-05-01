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
                            logger "env.no_proxy contains ${sonarHost}? ${env.no_proxy.contains(sonarHost)}"
                            if (!env.no_proxy || !env.no_proxy.contains(sonarHost)) {
                                if (PipelineSettings.ProxySettings.proxyHost && PipelineSettings.ProxySettings.proxyPort) {
                                    logger "setting sonar proxy ${PipelineSettings.ProxySettings.proxyHost}:${PipelineSettings.ProxySettings.proxyPort}"
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