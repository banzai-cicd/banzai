package com.ge.nola;
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent

class BaseBanzaiStage {
    String stageName
    BanzaiCfg cfg
    String validationMessage

    def validate(Closure c) {
        this.validationMessage = c.call()
    }

    def execute(Closure c) {
        if (this.validationMessage) {
            logger this.validationMessage
            return
        }

        stage (this.stageName) {
            try {
                notify(cfg, [
                    scope: BanzaiEvent.scope.STAGE,
                    status: BanzaiEvent.status.PENDING,
                    stage: this.stageName,
                    message: 'Pending'
                ])
                c.call()
                notify(cfg, [
                    scope: BanzaiEvent.scope.STAGE,
                    status: BanzaiEvent.status.SUCCESS,
                    stage: this.stageName,
                    message: 'Success'
                ])
            } catch (err) {
                echo "Caught: ${err}"
                currentBuild.result = 'FAILURE'
                if (isGithubError(err)) {
                    notify(cfg, [
                        scope: BanzaiEvent.scope.STAGE,
                        status: BanzaiEvent.status.FAILURE,
                        stage: this.stageName,
                        message: 'githubdown'
                    ])
                } else {
                    notify(cfg, [
                        scope: BanzaiEvent.scope.STAGE,
                        status: BanzaiEvent.status.FAILURE,
                        stage: this.stageName,
                        message: 'Failed'
                    ])   
                }
                
                error(err.message)
            }
        }
    }
}