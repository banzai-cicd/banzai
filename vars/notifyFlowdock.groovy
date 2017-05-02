#!/usr/bin/env groovy

import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result
import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config, stage, message) {
    if (!config.mergeBranches || !config.flowdockCredId || !config.flowdockAuthor) {
      println "'mergeBranches', 'flowdockFlowToken' and 'flowdockAuthor' are required in your Jenkinsfile when 'flowdock' = true"
      return
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.flowdockCredId,
                               usernameVariable: 'FLOWDOCK_USER', passwordVariable: 'FLOWDOCK_PASSWORD']]) {
       def flowdockURL = "https://api.flowdock.com/messages"

       // determine if this is a merge or pr (should use diff threads)
       Pattern mergePattern = Pattern.compile(config.mergeBranches)
       def threadId = "${config.appName}+${env.JOB_BASE_NAME}"
       def title = env.JOB_BASE_NAME
       if ((BRANCH_NAME ==~ mergePattern)) {
         title = "${title} Merge"
       } else {
         if (!config.flowdockNotifyPRs) {
           // by default, we don't want to bug people in flowdock with PR's
           return
         }

         threadId = "${threadId}+merge"
       }

       def color = "green"
       if (!currentBuild.result) {
         color = "yellow"
       } else if (currentBuild.result == "FAILURE" || currentBuild.result == "UNSTABLE") {
         color = "red"
       }

       def payloadMap = [
         flow_token: FLOWDOCK_PASSWORD,
         event: "activity",
         author: config.flowdockAuthor,
         title: "${config.appName} : ${stage}",
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

       def payload = JsonOutput.toJson(payloadMap)

       println(payload)

       sh """#!/bin/bash
         echo "Sending Flowdock notification..."
         curl -H \"Content-Type: application/json\" -X POST -s -d \'${payload}\' ${flowdockURL}
         """
    }

}
