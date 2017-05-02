#!/usr/bin/env groovy

import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result

def call(config, stage, message, status) {
    // build status of null means successful
    // StringBuilder content = new StringBuilder();
    // content.append("<h3>").append(env.JOB_BASE_NAME).append("</h3>");
    // content.append("Build: ").append(currentBuild.displayName).append("<br />");
    // content.append("Result: <strong>").append(status).append("</strong><br />");
    // content.append("URL: <a href=\"").append(env.BUILD_URL).append("\">").append(currentBuild.fullDisplayName).append("</a>").append("<br />");

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.flowdockFlowToken,
                               usernameVariable: 'FLOWDOCK_USER', passwordVariable: 'FLOWDOCK_PASSWORD']]) {
       def flowdockURL = "https://api.flowdock.com/messages"

       def color = "green"
       if (status == "PENDING") {
         color = "yellow"
       } else if (status == "FAILURE") {
         color = "red"
       }

       def payloadMap = [
         flow_token: FLOWDOCK_PASSWORD,
         event: "activity",
         author: config.flowdockAuthor,
         title: "${config.appName} : <a href='${BUILD_URL}'>${stage} ${status}</a>",
         body: "<b>${currentBuild.displayName.replaceAll("#", "")}</b> - ${message}",
         external_thread_id: "${config.appName}+${env.JOB_BASE_NAME}".bytes.encodeBase64().toString(),
         thread: [
          title: env.JOB_BASE_NAME,
          status: [
            color: color,
            value: status
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
