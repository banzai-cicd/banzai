#!/usr/bin/env groovy
import main.groovy.cicd.pipeline.settings.PipelineSettings;
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiQualityCfg
import com.ge.nola.BanzaiEvent
import com.ge.nola.BanzaiStage

def call(BanzaiCfg cfg, List<BanzaiQualityCfg> scanConfigs) {
    def stages = [:]
    def pipeline = this
    scanConfigs.each {
        switch (it.type) {
            case "sonar": // requires https://github.build.ge.com/PowerDevOps/jenkins-master-shared-library
                stages[it.type] = {
                    String stageName = "Sonar"
                    BanzaiStage banzaiStage = new BanzaiStage(
                        pipeline: pipeline,
                        cfg: cfg,
                        stageName: stageName
                    )
                    banzaiStage.execute {
                        try {
                            sonarqubeQualityCheck();

                            def proxyOn = false
                            def sonarHost = PipelineSettings.SonarQubeSettings.sonarHostUrl.replaceFirst(/(http|https):\/\//, "")
                            if ((!cfg.noProxy || !cfg.noProxy.contains(sonarHost)) && cfg.httpsProxy) {
                                logger "setting sonar proxy ${cfg.httpsProxy.host}:${cfg.httpsProxy.port}"
                                proxyOn = true
                            }
                            
                            sonarqubeQualityResults(proxyOn);
                        } catch (Exception e) {
                            echo "Caught: ${e}"
                            currentBuild.result = 'UNSTABLE'
                            String abort = it.abortOnError ? "true" : "false"
                            throw new Exception(abort) // let the scansStage know if it should abort
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