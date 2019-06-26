#!/usr/bin/env groovy
import com.ge.nola.banzai.cfg.BanzaiCfg
import com.ge.nola.banzai.BanzaiStage

def call(BanzaiCfg cfg) {
  if (cfg.skipSCM == true) { return }

  String stageName = 'Checkout'
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )

  banzaiStage.execute {
    checkout([
      $class: 'GitSCM',
      branches: scm.branches,
      doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
      extensions: scm.extensions + [$class: 'LocalBranch', localBranch: "**"],
      userRemoteConfigs: scm.userRemoteConfigs
    ])
  }
}
