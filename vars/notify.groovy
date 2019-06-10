#!/usr/bin/env groovy

import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiEvent

void call(BanzaiCfg cfg, Map eventOpts) {
  BanzaiEvent event = new BanzaiEvent(eventOpts)
  try {
    if (event.message != 'githubdown') {
      notifyGit(cfg, event)
    }
    notifyFlowdock(cfg, event)
    notifyEmail(cfg, event)    
  } catch (Exception e) {
    error(e.message)
  }
}