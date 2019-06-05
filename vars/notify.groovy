#!/usr/bin/env groovy

import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent

void call(BanzaiCfg cfg, Map eventOpts) {
  BanzaiEvent event = new BanzaiEvent(eventOpts)
  try {
    if (event.message != 'githubdown') {
      notifyGit(cfg, event)
    }
    notifyFlowdock(cfg, event)
    // notifyEmail(cfg, event)    
  } catch (Exception e) {
    error(e.message)
  }
}


// def call(config, stage, message, status, enableGit=true) {
//   try {
//     if (config.flowdock) {
//       notifyFlowdock(config, stage, message, status)
//     }

//     if (enableGit) {
//       notifyGit(config, stage, message, status)
//     }
//   } catch (Exception e) {
//     error(e.message)
//   }
// }
