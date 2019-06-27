#!/usr/bin/env groovy

def call(String from, String to, String subject, String body, String attachmentsPattern) {
  logger "Sending Email to ${to}: ${subject}"
  String url = env.RUN_DISPLAY_URL ?: env.BUILD_URL
  String jobInfo = "Job: ${env.JOB_NAME} #${env.BUILD_NUMBER} \nBuild URL: ${url}\n\n"
  emailext from: from,
    to: to,
    subject:"Banzai: ${subject}",
    body: "${jobInfo}${body}",
    attachmentsPattern: attachmentsPattern
}