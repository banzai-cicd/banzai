#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiStepCfg
import com.ge.nola.BanzaiStage

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