#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg

def call(BanzaiCfg cfg) {
  def stageName = 'Checkout'

  if (cfg.skipSCM == false) {
    try {
        notify(cfg, stageName, 'Pending', 'PENDING')
        checkout([
          $class: 'GitSCM',
          branches: scm.branches,
          doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
          extensions: scm.extensions + [$class: 'LocalBranch', localBranch: "**"],
          userRemoteConfigs: scm.userRemoteConfigs
        ])
        notify(cfg, stageName, 'Successful', 'PENDING')
    } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'UNSTABLE'
        if (isGithubError(err)) {
            notify(cfg, stageName, 'githubdown', 'FAILURE', true)
        } else {
            notify(cfg, stageName, 'Failed', 'FAILURE')
        }
        error(err.message)
    }
  }

}
