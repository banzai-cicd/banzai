#!/usr/bin/env groovy
import main.groovy.cicd.pipeline.settings.PipelineSettings;
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiQualityCfg

def call(BanzaiCfg cfg, scanConfigs) {
    def stages = [:]
    
    scanConfigs.each {
        switch (it.type) {
            case "sonar": // requires https://github.build.ge.com/PowerDevOps/jenkins-master-shared-library
                stages[it.type] = {
                    stage("Sonar") {
                        try {
                            notify(cfg, 'Sonar', 'Pending', 'PENDING')
                            sonarqubeQualityCheck();

                            def proxyOn = false
                            def sonarHost = PipelineSettings.SonarQubeSettings.sonarHostUrl.replaceFirst(/(http|https):\/\//, "")
                            if ((!cfg.noProxy || !cfg.noProxy.contains(sonarHost)) && cfg.httpsProxy) {
                                logger "setting sonar proxy ${cfg.httpsProxy.host}:${cfg.httpsProxy.port}"
                                proxyOn = true
                            }
                            
                            sonarqubeQualityResults(proxyOn);

                            notify(cfg, 'Sonar', 'Successful', 'PENDING')
                        } catch (err) {
                            echo "Caught: ${err}"
                            currentBuild.result = 'UNSTABLE'
                            notify(cfg, 'Sonar', 'Failed', 'FAILURE')
                            def abort = it.abortOnError ? "true" : "false"
                            error(abort) // let the scansStage know if it should abort
                        }
                    }
                }
                break
            default:
                logger("Unable to parse qualityScans cfg item: ${it}")
                break
        }
    }

    parallel(stages)
}