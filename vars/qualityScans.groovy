#!/usr/bin/env groovy
import main.groovy.cicd.pipeline.settings.PipelineSettings;
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiQualityCfg
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg, List<BanzaiQualityCfg> scanConfigs) {
    def stages = [:]
    
    String stageName
    scanConfigs.each {
        switch (it.type) {
            case "sonar": // requires https://github.build.ge.com/PowerDevOps/jenkins-master-shared-library
                stageName = "Sonar"
                stages[it.type] = {
                    stage(stageName) {
                        try {
                            notify(cfg, [
                                scope: BanzaiEvent.Scope.STAGE,
                                status: BanzaiEvent.Status.PENDING,
                                stage: stageName,
                                message: 'Pending'
                            ])
                            sonarqubeQualityCheck();

                            def proxyOn = false
                            def sonarHost = PipelineSettings.SonarQubeSettings.sonarHostUrl.replaceFirst(/(http|https):\/\//, "")
                            if ((!cfg.noProxy || !cfg.noProxy.contains(sonarHost)) && cfg.httpsProxy) {
                                logger "setting sonar proxy ${cfg.httpsProxy.host}:${cfg.httpsProxy.port}"
                                proxyOn = true
                            }
                            
                            sonarqubeQualityResults(proxyOn);

                            notify(cfg, [
                                scope: BanzaiEvent.Scope.STAGE,
                                status: BanzaiEvent.Status.SUCCESS,
                                stage: stageName,
                                message: 'Success'
                            ])
                        } catch (err) {
                            echo "Caught: ${err}"
                            currentBuild.result = 'UNSTABLE'
                            notify(cfg, [
                                scope: BanzaiEvent.Scope.STAGE,
                                status: BanzaiEvent.Status.FAILURE,
                                stage: stageName,
                                message: 'Failed'
                            ])
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