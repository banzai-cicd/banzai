#!/usr/bin/env groovy

import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent

/*
  By default, match PIPELINE:SUCCESS and all STAGE events except STAGE:SUCCESS
  (we don't want to mark the build as a success after each stage, only after PIPELINE)
*/
final Map DEFAULT_EVENT_MAP = [
  /${BanzaiEvent.scope.PIPELINE}:${BanzaiEvent.status.SUCCESS}/ : true,
  /^${BanzaiEvent.scope.STAGE}:(?!(${BanzaiEvent.status.SUCCESS})$).*$/ : true
]

def call (BanzaiCfg cfg, BanzaiEvent event) {
  if (findValueInRegexObject(DEFAULT_EVENT_MAP, "${event.scope}:${event.status}")) {
    githubNotify description: "${event.stage}: ${event.message}", context: "Banzai", status: "${event.status}", gitApiUrl: GITHUB_API_URL
  }
}

// def call(config, stage, msg, status) {
//   //githubNotify description: msg,  status: status, credentialsId: config.gitTokenId, account: config.gitAccount, gitApiUrl: GITHUB_API_URL
//   // infer credentials and account from build info
//   githubNotify description: "${stage}: ${msg}",  context: "Banzai", status: status, gitApiUrl: GITHUB_API_URL
// }