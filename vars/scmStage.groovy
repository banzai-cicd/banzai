#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg

def call(BanzaiCfg cfg) {
  def stageName = 'Checkout'

  if (cfg.skipSCM == false) {
    try {
        notify(cfg, [
            scope: BanzaiEvent.scope.STAGE,
            status: BanzaiEvent.status.PENDING,
            stage: stageName,
            message: 'Pending'
        ])
        checkout([
          $class: 'GitSCM',
          branches: scm.branches,
          doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
          extensions: scm.extensions + [$class: 'LocalBranch', localBranch: "**"],
          userRemoteConfigs: scm.userRemoteConfigs
        ])
        notify(cfg, [
            scope: BanzaiEvent.scope.STAGE,
            status: BanzaiEvent.status.SUCCESS,
            stage: stageName,
            message: 'Success'
        ])
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'UNSTABLE'
        if (isGithubError(err)) {
            notify(cfg, [
                scope: BanzaiEvent.scope.STAGE,
                status: BanzaiEvent.status.FAILURE,
                stage: stageName,
                message: 'githubdown'
            ])
        } else {
            notify(cfg, [
                scope: BanzaiEvent.scope.STAGE,
                status: BanzaiEvent.status.FAILURE,
                stage: stageName,
                message: 'Failed'
            ])   
        }
        error(err.message)
    }
  }
}
