#!/usr/bin/env groovy
import groovy.json.JsonOutput
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiFlowdockCfg

def call(BanzaiCfg config, String stage, String message, String status) {
    BanzaiFlowdockCfg flockdockCfg = getBranchBasedConfig(config.flowdock)

    if (flockdockCfg == null) {
      logger "${BRANCH_NAME} does not match a 'flowdock' branch pattern. Skipping flowdock notification"
      return
    }

    if (!flockdockCfg.flowdockCredId || !flockdockCfg.flowdockAuthor) {
      logger "'flowdockCredId' and 'flowdockAuthor' are required in your .banzai when 'flowdock' branches are defined"
      return
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: flockdockCfg.credId,
                               usernameVariable: 'FLOWDOCK_USER', passwordVariable: 'FLOWDOCK_PASSWORD']]) {
       def flowdockURL = "https://api.flowdock.com/messages"

       // determine if this is a merge or pr (should use diff threads)
       def threadId = "${flockdockCfg.appName}+${env.JOB_BASE_NAME}"
       def title = "${flockdockCfg.appName} : ${env.JOB_BASE_NAME}"
       if (!BRANCH_NAME.startsWith('PR-')) {
         title = "${title} : merge"
       } else {
         if (!flockdockCfg.notifyPRs && message != "githubdown" && stage != "IT") {
           // by default, we don't want to bug people in flowdock with PR's
           return
         }

         threadId = "${threadId}+pr"
       }

       def color 
       switch (status) {
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
         author: flockdockCfg.author,
         title: "${stage}",
         body: "<a href='${BUILD_URL}'><b>${currentBuild.displayName.replaceAll("#", "")}</b> - ${message}</a>",
         external_thread_id: threadId.bytes.encodeBase64().toString(),
         thread: [
          title: title,
          status: [
            color: color,
            value: currentBuild.result ? currentBuild.result : "PENDING"
          ]
         ],
         link: env.BUILD_URL
       ]

       if (message == "githubdown") {
         payloadMap.body = "https://image.ibb.co/bUkDfv/the_ge_github_is_down.png"
       }

       def payload = JsonOutput.toJson(payloadMap)

       if (config.debug) {
         logger(payload)
       }

       logger "Sending Flowdock notification : ${stage} : ${message}"

       sh """#!/bin/bash
         curl -H \"Content-Type: application/json\" -X POST -s -d \'${payload}\' ${flowdockURL}
         """
    }

}
