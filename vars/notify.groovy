#!/usr/bin/env groovy

import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiEvent

void call(BanzaiCfg cfg, Map eventOpts) {
  BanzaiEvent event = new BanzaiEvent(eventOpts)
  if (event.scope == BanzaiEvent.Scope.PIPELINE && event.status == BanzaiEvent.Status.FAILURE) {
    cfg.internal.PIPELINE_FAILED = true
  }

  try {
    if (cfg.internal.PIPELINE_FAILED == false && event.message != 'githubdown') {
      /*
        only notify git if github is not down && the pipeline hasn't
        already been marked as a failure
      */
      notifyGit(cfg, event)
    }

    notifyFlowdock(cfg, event)
    notifyEmail(cfg, event)  
  } catch (Exception e) {
    error(e.message)
  }
}