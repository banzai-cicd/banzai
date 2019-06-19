#!/usr/bin/env groovy

import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiEvent

void call(BanzaiCfg cfg, Map eventOpts) {
  BanzaiEvent event = new BanzaiEvent(eventOpts)
  try {
    if (event.message != 'githubdown' 
      || cfg.internal.PIPELINE_FAILURE_NOTIF_SENT != true) {
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
  } finally {
    if (event.scope == BanzaiEvent.Scope.PIPELINE 
      && event.status == BanzaiEvent.Status.FAILURE) {
        /*
          once the pipeline has been marked for failure, stop sending notifications to
          github that may change the status to something other than FAILURE. ie) a successful
          cleanWorkspace.post stage.
        */
        cfg.internal.PIPELINE_FAILURE_NOTIF_SENT = true
      }
  }
}