package com.ge.nola;
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent
import org.jenkinsci.plugins.workflow.cps.CpsClosure2

class BanzaiBaseStage {
    String stageName
    BanzaiCfg cfg
    String validationMessage

    def validate(CpsClosure2 c) {
        this.validationMessage = c.call()
    }

    def execute(CpsClosure2 c) {
        if (this.validationMessage) {
            logger this.validationMessage
            return
        }

        stage (this.stageName) {
            try {
                notify(cfg, [
                    scope: BanzaiEvent.Scope.STAGE,
                    status: BanzaiEvent.Status.PENDING,
                    stage: this.stageName,
                    message: 'Pending'
                ])
                c.call()
                notify(cfg, [
                    scope: BanzaiEvent.Scope.STAGE,
                    status: BanzaiEvent.Status.SUCCESS,
                    stage: this.stageName,
                    message: 'Success'
                ])
            } catch (err) {
                echo "Caught: ${err}"
                currentBuild.result = 'FAILURE'
                if (isGithubError(err)) {
                    notify(cfg, [
                        scope: BanzaiEvent.Scope.STAGE,
                        status: BanzaiEvent.Status.FAILURE,
                        stage: this.stageName,
                        message: 'githubdown'
                    ])
                } else {
                    notify(cfg, [
                        scope: BanzaiEvent.Scope.STAGE,
                        status: BanzaiEvent.Status.FAILURE,
                        stage: this.stageName,
                        message: 'Failed'
                    ])   
                }
                
                error(err.message)
            }
        }
    }
}