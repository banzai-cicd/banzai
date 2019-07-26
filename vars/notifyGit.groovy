#!/usr/bin/env groovy

import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.BanzaiEvent
import java.net.URI

String getHostName(String url) {
    logger "getHostName for ${url}"
    if (url.contains("git@")) {
        return url.replaceFirst("git@", "").tokenize(":")[0]
    } else {
        URI uri = new URI(url)    
        String hostname = uri.getHost()
        // to provide faultproof result, check if not null then return only hostname, without www.
        if (hostname != null) {
            return hostname.startsWith("www.") ? hostname.substring(4) : hostname
        }
        return hostname
    }
}

void call(BanzaiCfg cfg, BanzaiEvent event) {
  logger "notifyGit called"
  String hostName = getHostName(scm.getUserRemoteConfigs()[0].getUrl())
  String GITHUB_API_URL = "https://${hostName}/api/v3"
  logger "notifiyGit url: ${GITHUB_API_URL}"
  /*
    Stage Events that reach github: PENDING, FAILURE, ABORTED
    Pipeline Events that reach github: SUCCESS
    ie) we don't want to mark the build as a success after each stage, only after PIPELINE
    and in the event of a failure we want the stage that failed to remain the last notification
    sent (not the PIPELINE:FAILURE) as this is more helpful feedback.
  */
  Map DEFAULT_EVENT_MAP = [:]
  DEFAULT_EVENT_MAP[/$BanzaiEvent.Scope.PIPELINE:$BanzaiEvent.Status.SUCCESS/] = true
  DEFAULT_EVENT_MAP[/^$BanzaiEvent.Scope.STAGE:(?!($BanzaiEvent.Status.SUCCESS)$).*$/] = true

  if (findValueInRegexObject(DEFAULT_EVENT_MAP, "${event.scope}:${event.status}")) {
    logger "githubNotify ${event.stage} : ${event.message} : ${event.stage} "
    githubNotify description: "${event.stage}: ${event.message}", context: "Banzai", status: "${event.status}", gitApiUrl: GITHUB_API_URL, credentialsId: cfg.gitTokenId
  }
}

// def call(config, stage, msg, status) {
//   //githubNotify description: msg,  status: status, credentialsId: config.gitTokenId, account: config.gitAccount, gitApiUrl: GITHUB_API_URL
//   // infer credentials and account from build info
//   githubNotify description: "${stage}: ${msg}",  context: "Banzai", status: status, gitApiUrl: GITHUB_API_URL
// }
