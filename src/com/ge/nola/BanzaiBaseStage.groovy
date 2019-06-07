package com.ge.nola;

import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent
import org.jenkinsci.plugins.workflow.cps.CpsClosure2
import org.codehaus.groovy.runtime.GStringImpl

class BanzaiBaseStage {
    def pipeline
    String stageName
    BanzaiCfg cfg
    GStringImpl validationMessage

    def validate(CpsClosure2 c) {
        def ret = c.call()
        if (ret instanceof GStringImpl) { // to avoid accidental implcit returns
            validationMessage = ret
        }
    }

    def execute(CpsClosure2 c) {
        if (validationMessage) {
            logger validationMessage
            return
        }

        pipeline.stage (stageName) {
            try {
                pipeline.notify(cfg, [
                    scope: BanzaiEvent.Scope.STAGE,
                    status: BanzaiEvent.Status.PENDING,
                    stage: stageName,
                    message: 'Pending'
                ])
                c.call()
                pipeline.notify(cfg, [
                    scope: BanzaiEvent.Scope.STAGE,
                    status: BanzaiEvent.Status.SUCCESS,
                    stage: stageName,
                    message: 'Success'
                ])
            } catch (err) {
                pipeline.logger "Caught: ${err}"
                pipeline.currentBuild.result = 'FAILURE'
                if (pipeline.isGithubError(err)) {
                    pipeline.notify(cfg, [
                        scope: BanzaiEvent.Scope.STAGE,
                        status: BanzaiEvent.Status.FAILURE,
                        stage: stageName,
                        message: 'githubdown'
                    ])
                } else {
                    pipeline.notify(cfg, [
                        scope: BanzaiEvent.Scope.STAGE,
                        status: BanzaiEvent.Status.FAILURE,
                        stage: stageName,
                        message: 'Failed'
                    ])   
                }
                
                pipeline.error(err.message)
            }
        }
    }
}