#!/usr/bin/env groovy

import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.BanzaiEvent

void call(BanzaiCfg cfg, BanzaiEvent event) {
  logger "notifyGit called"
  String GITHUB_API_URL = 'https://github.build.ge.com/api/v3'
  /*
    By default, match PIPELINE:SUCCESS and all STAGE events except STAGE:SUCCESS
    (we don't want to mark the build as a success after each stage, only after PIPELINE)
  */
  Map DEFAULT_EVENT_MAP = [:]
  DEFAULT_EVENT_MAP[/${BanzaiEvent.Scope.PIPELINE}:${BanzaiEvent.Status.SUCCESS}/] = true
  DEFAULT_EVENT_MAP[/^${BanzaiEvent.Scope.STAGE}:(?!(${BanzaiEvent.Status.SUCCESS})$).*$/] = true

  logger "${event.scope}:${event.status}"
  if (findValueInRegexObject(DEFAULT_EVENT_MAP, "${event.scope}:${event.status}")) {
    logger "githubNotify ${event.stage} : ${event.message} : ${event.stage} "
    githubNotify description: "${event.stage}: ${event.message}", context: "Banzai", status: "${event.status}", gitApiUrl: GITHUB_API_URL
  }
}

// def call(config, stage, msg, status) {
//   //githubNotify description: msg,  status: status, credentialsId: config.gitTokenId, account: config.gitAccount, gitApiUrl: GITHUB_API_URL
//   // infer credentials and account from build info
//   githubNotify description: "${stage}: ${msg}",  context: "Banzai", status: status, gitApiUrl: GITHUB_API_URL
// }