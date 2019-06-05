#!/usr/bin/env groovy

def call(to, cc, subject, body) {
  logger "Sending Email to ${to}: ${subject}"
  String url = env.RUN_DISPLAY_URL ?: env.BUILD_URL
  String jobInfo = "Job: ${env.JOB_NAME} #${env.BUILD_NUMBER} \nBuild URL: ${url}\n\n"
  mail from: "JenkinsAdmin@ge.com",
    to: to,
    cc: cc,
    subject: subject,
    body: "${jobInfo}${body}"
}