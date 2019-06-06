#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent

def call(BanzaiCfg cfg) {
  def stageName = 'Checkout'

  if (cfg.skipSCM == false) {
    try {
        notify(cfg, [
            scope: BanzaiEvent.Scope.STAGE,
            status: BanzaiEvent.Status.PENDING,
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
            scope: BanzaiEvent.Scope.STAGE,
            status: BanzaiEvent.Status.SUCCESS,
            stage: stageName,
            message: 'Success'
        ])
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'UNSTABLE'
        if (isGithubError(err)) {
            notify(cfg, [
                scope: BanzaiEvent.Scope.STAGE,
                status: BanzaiEvent.Status.FAILURE,
                stage: stageName,
                message: 'githubdown'
            ])
        } else {
            notify(cfg, [
                scope: BanzaiEvent.Scope.STAGE,
                status: BanzaiEvent.Status.FAILURE,
                stage: stageName,
                message: 'Failed'
            ])   
        }
        error(err.message)
    }
  }
}
