#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiQualityCfg
import com.ge.nola.BanzaiStage
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg, List<BanzaiQualityCfg> scanConfigs) {
    def stages = [:]
    def pipeline = this
    scanConfigs.each {
        switch (it.type) {
            case "sonar":
                stages[it.type] = {
                    String stageName = "Sonar"
                    BanzaiStage banzaiStage = new BanzaiStage(
                        pipeline: pipeline,
                        cfg: cfg,
                        stageName: stageName
                    )
                    banzaiStage.execute {
                        try {
                            sonarQube(cfg, it);
                            notify(cfg, [
                                scope: BanzaiEvent.Scope.QUALITY,
                                status: BanzaiEvent.Status.SUCCESS,
                                stage: stageName,
                                message: 'Sonar Scan Success'
                            ])
                        } catch (Exception e) {
                            logger "${e.message}"
                            notify(cfg, [
                                scope: BanzaiEvent.Scope.QUALITY,
                                status: BanzaiEvent.Status.FAILURE,
                                stage: stageName,
                                message: 'Sonar Scan Failure'
                            ])
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