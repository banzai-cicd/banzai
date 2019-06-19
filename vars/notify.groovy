#!/usr/bin/env groovy

import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiEvent

void call(BanzaiCfg cfg, Map eventOpts) {
  BanzaiEvent event = new BanzaiEvent(eventOpts)
  if (event.scope == BanzaiEvent.Scope.PIPELINE && event.status == BanzaiEvent.Status.FAILURE) {
    logger "Pipeline failed"
    cfg.internal.PIPELINE_FAILED = true
  }

  logger "cfg.internal.PIPELINE_FAILED: ${cfg.internal.PIPELINE_FAILED}"
  try {
    if (event.message != 'githubdown'
      || cfg.internal.PIPELINE_FAILED == false) {
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