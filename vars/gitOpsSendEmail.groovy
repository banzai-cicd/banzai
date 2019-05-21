#!/usr/bin/env groovy

def call(to, cc, subject, body) {
  logger "Sending Email to ${to}: ${subject}"
  String jobInfo = "Job: ${env.JOB_NAME} #${env.BUILD_NUMBER} \nBuild URL: ${env.BUILD_URL}\n\n"
  mail from: "JenkinsAdmin@ge.com",
    to: to,
    cc: cc,
    subject: subject,
    body: "${jobInfo}${body}"
}