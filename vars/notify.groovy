#!/usr/bin/env groovy

def call(config, stage, message, status, skipGit=false) {
  if (config.flowdock) {
    notifyFlowdock(config, stage, message)
  }
}
