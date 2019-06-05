#!/usr/bin/env groovy
import groovy.json.JsonOutput
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiFlowdockCfg
import com.ge.nola.BanzaiEvent

//def call(BanzaiCfg config, String stage, String message, String status) {
void call(BanzaiCfg cfg, BanzaiEvent event) {
  if (!cfg.flowdock || !cfg.notifications || !cfg.notifications.flowdock) {
    return
  }
  
  // see if there is a notifications.flowdock configuration for the current branch
  Map<String, List<String>> flowdockNotificationsCfg = 
    findValueInRegexObject(cfg.notifications.flowdock, BRANCH_NAME)
  if (flowdockNotificationsCfg == null) {
    logger "flowdockNotificationsCfg = null"
    return
  }
  /*
  get a list of flowCfgId's that have a regex matching the current event
  */
  String currentEvent = event.getEventLabel()
  Set<String> flowConfigIds = flowdockNotificationsCfg.keySet().findAll { flowCfgId ->
      flowdockNotificationsCfg[flowCfgId].find { regex -> currentEvent ==~ regex }
  }

  if (flowConfigIds == null) {
    logger "flowConfigIds = null"
    return
  }

  // now we know we'd like to send some notifications
  List<BanzaiFlowdockCfg> flowdockCfgs = flowConfigIds.collect { cfg.flowdock[it] }
  flowdockCfgs.each { BanzaiFlowdockCfg flowdockCfg ->
    if (!flowdockCfg.credId || !flowdockCfg.author) {
      logger "'flowdock.credId' and 'flowdock.author' are required in your .banzai when 'flowdock' branches are defined"
      return
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: flowdockCfg.credId,
                              usernameVariable: 'FLOWDOCK_USER', passwordVariable: 'FLOWDOCK_PASSWORD']]) {
      def flowdockURL = "https://api.flowdock.com/messages"

      def threadId = "${cfg.appName}+${env.BRANCH_NAME}"
      def title = "${cfg.appName} : ${env.BRANCH_NAME}"
      // if (!BRANCH_NAME.startsWith('PR-')) {
      //   title = "${title} : merge"
      // } else {
      //   if (!flowdockCfg.notifyPRs && event.message != "githubdown") {
      //     // by default, we don't want to bug people in flowdock with PR's
      //     return
      //   }

      //   threadId = "${threadId}+pr"
      // }

      def color 
      switch (event.status) {
        case "PENDING":
            color = "yellow"
            break
        case "FAILURE":
            color = "red"
            break
        default:
          color = 'green'
          break
      }   

      def payloadMap = [
        flow_token: FLOWDOCK_PASSWORD,
        event: "activity",
        author: flowdockCfg.author,
        title: "${event.stage}", // title of this specific message
        body: "<a href='${BUILD_URL}'><b>${currentBuild.displayName.replaceAll("#", "")}</b> - ${event.message}</a>",
        external_thread_id: threadId.bytes.encodeBase64().toString(),
        thread: [
          title: title, // title of the whole thread
          status: [
            color: color,
            value: event.status
          ]
        ],
        link: env.BUILD_URL
      ]

      if (event.message == "githubdown") {
        payloadMap.body = "https://image.ibb.co/bUkDfv/the_ge_github_is_down.png"
      }

      def payload = JsonOutput.toJson(payloadMap)

      if (cfg.debug) {
        logger(payload)
      }

      logger "Sending Flowdock notification : ${event.stage} : ${event.message}"

      sh """#!/bin/bash
        curl -H \"Content-Type: application/json\" -X POST -s -d \'${payload}\' ${flowdockURL}
        """
    }
  }
}