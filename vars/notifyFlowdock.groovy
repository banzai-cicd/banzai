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
    def flowdockURL = "https://api.flowdock.com/v1/messages/team_inbox/${apiToken}"
    def payload = JsonOutput.toJson([source : "Jenkins",
                                     project : env.JOB_BASE_NAME,
                                     from_address: conf.flowdockEmail,
                                     from_name: 'CI',
                                     subject: subject,
                                     content: content,
                                     tags: [":user:everyone", "#build"],
                                     link: env.BUILD_URL
                  ])

    sh """#!/bin/bash
      echo "Sending Flowdock notification..."
      curl -H \"Content-Type: application/json\" -X POST -s -d \'${payload}\' ${flowdockURL}
      """
}
