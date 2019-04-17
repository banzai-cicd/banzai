#!/usr/bin/env groovy
import groovy.json.JsonOutput

import java.util.regex.Pattern

def call(config, stage, message) {
    if (!config.mergeBranches || !config.flowdockCredId || !config.flowdockAuthor) {
      logger "'mergeBranches', 'flowdockFlowToken' and 'flowdockAuthor' are required in your Jenkinsfile when 'flowdock' = true"
      return
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.flowdockCredId,
                               usernameVariable: 'FLOWDOCK_USER', passwordVariable: 'FLOWDOCK_PASSWORD']]) {
       def flowdockURL = "https://api.flowdock.com/messages"

       // determine if this is a merge or pr (should use diff threads)
       Pattern mergePattern = Pattern.compile(config.mergeBranches)
       def threadId = "${config.appName}+${env.JOB_BASE_NAME}"
       def title = "${config.appName} : ${env.JOB_BASE_NAME}"
       if ((BRANCH_NAME ==~ mergePattern)) {
         title = "${title} : merge"
       } else {
         if (!config.flowdockNotifyPRs && message != "githubdown" && stage != "IT") {
           // by default, we don't want to bug people in flowdock with PR's
           return
         }

         threadId = "${threadId}+pr"
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
        // TODO REVISIT WITH A SOLUTION TO geGithubIsDown() string exceeding max chars
        //  payloadMap.event = "file"
        // //  payloadMap.attachments = [
        // //    [
        // //     file_name: "ge_github_is_down.png",
        // //     data: geGithubIsDown(),
        // //     content_type: "image/png"
        // //    ]
        // //  ]
        //  payloadMap.content = [
        //   file_name: "ge_github_is_down.png",
        //   data: geGithubIsDown(),
        //   content_type: "image/png"
        //  ]
        //  payloadMap.tags = [":file"]
        //  payloadMap.remove("body")
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
