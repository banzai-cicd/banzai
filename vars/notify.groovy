#!/usr/bin/env groovy

def call(config, stage, message, status, skipGit=false) {
  try {
    if (config.flowdock) {
      notifyFlowdock(config, stage, message)
    }

    if (notifyGit != false && !skipGit) {
      notifyGit(config, stage, "${message}", status)
    }
  } catch (Exception e) {
    error(e.message)
  }
}
