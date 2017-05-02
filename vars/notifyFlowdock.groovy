#!/usr/bin/env groovy

import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result

def call(config, message, status) {
    // build status of null means successful
    def subject = "${env.JOB_BASE_NAME} build ${currentBuild.displayName.replaceAll("#", "")} : ${status}"

    StringBuilder content = new StringBuilder();
    content.append("<h3>").append(env.JOB_BASE_NAME).append("</h3>");
    content.append("Build: ").append(currentBuild.displayName).append("<br />");
    content.append("Result: <strong>").append(status).append("</strong><br />");
    content.append("URL: <a href=\"").append(env.BUILD_URL).append("\">").append(currentBuild.fullDisplayName).append("</a>").append("<br />");

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.flowdockCredId,
                               usernameVariable: 'FLOWDOCK_USER', passwordVariable: 'FLOWDOCK_PASSWORD']]) {

       def flowdockURL = "https://${FLOWDOCK_PASSWORD}@api.flowdock.com/flows/${config.flowdockOrg}/${config.flowdockFlow}"
       def payload = JsonOutput.toJson([event: "message",
                                        content: content,
                                        tags: ["@team", "#build"],
                                        link: env.BUILD_URL
                     ])

       sh """#!/bin/bash
         echo "Sending Flowdock notification..."
         curl -H \"Content-Type: application/json\" -X POST -s -d \'${payload}\' ${flowdockURL}
         """
    }

}
