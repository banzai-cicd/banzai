package com.ge.nola;

import com.ge.nola.banzai.cfg.BanzaiCfg
import com.ge.nola.banzai.BanzaiEvent
import org.jenkinsci.plugins.workflow.cps.CpsClosure2
import org.codehaus.groovy.runtime.GStringImpl
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

class BanzaiStage {
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
        pipeline.logger stageName

        if (validationMessage) {
            pipeline.logger validationMessage
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
            } catch (FlowInterruptedException err) {
                pipeline.logger "Pipeline Aborted"
                pipeline.currentBuild.result = "${BanzaiEvent.Status.ABORTED}"
                pipeline.notify(cfg, [
                    scope: BanzaiEvent.Scope.STAGE,
                    status: BanzaiEvent.Status.FAILURE,
                    stage: stageName,
                    message: 'Aborted'
                ])
                throw err
            } catch (Exception err2) {
                pipeline.logger "Caught: ${err2}"
                /*
                    sometimes the originator of the error will
                    set the currentBuild.result to something other than
                    SUCCESS and we shouldn't overwrite that.
                */
                if (pipeline.currentBuild.result == "${BanzaiEvent.Status.SUCCESS}") { 
                    pipeline.currentBuild.result = "${BanzaiEvent.Status.FAILURE}"
                }

                if (pipeline.isGithubError(err2)) {
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
                
                pipeline.error(err2.message)
            }
        }
    }
}