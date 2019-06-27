#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiStepCfg
import com.github.banzaicicd.BanzaiStage

def call(BanzaiCfg cfg) {
  String stageName = 'Clean Workspace'
  BanzaiStage banzaiStage = new BanzaiStage(
    pipeline: this,
    cfg: cfg,
    stageName: stageName
  )

  banzaiStage.execute {
    step([$class: 'WsCleanup'])
  }
}