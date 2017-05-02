#!/usr/bin/env groovy

import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result

def call(config, message, status) {
    // build status of null means successful
    def title = "${env.JOB_BASE_NAME} build ${currentBuild.displayName.replaceAll("#", "")}"

    // StringBuilder content = new StringBuilder();
    // content.append("<h3>").append(env.JOB_BASE_NAME).append("</h3>");
    // content.append("Build: ").append(currentBuild.displayName).append("<br />");
    // content.append("Result: <strong>").append(status).append("</strong><br />");
    // content.append("URL: <a href=\"").append(env.BUILD_URL).append("\">").append(currentBuild.fullDisplayName).append("</a>").append("<br />");

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.flowdockFlowToken,
                               usernameVariable: 'FLOWDOCK_USER', passwordVariable: 'FLOWDOCK_PASSWORD']]) {

       def threadId = "123456789"
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
         title: "Build ${status}",
         external_thread_id: threadId,
         thread: [
          title: title,
          body: "this is a test body"
          status: [
            color: color,
            value: status
          ]
         ],
         content: content,
         link: env.BUILD_URL
       ]

       def payload = JsonOutput.toJson(payloadMap)

       sh """#!/bin/bash
         echo "Sending Flowdock notification..."
         curl -H \"Content-Type: application/json\" -X POST -s -d \'${payload}\' ${flowdockURL}
         """
    }

}
