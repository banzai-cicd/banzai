#!/usr/bin/env groovy
import java.io.File

def call(secretConfig) {
    logger "Filtering Secret: ${secretConfig.secretId}"
    logger secretConfig

    // sanitize the filename
    def file = secretConfig.file
    if (file.contains('..')) {
        error("Secret.file may not contain '..' and should be defined relative to the Jenkins Workspace")
        return
    }

    // copy target file to temp
    def filePath = "${env.WORKSPACE}/${file}"
    def tempFilePath = "${filePath}.temp"
    sh "mv ${filePath} ${tempFilePath}"

    // filter and replace deleted original file
    withCredentials([string(credentialsId: config[secretConfig.secretId], variable: 'SECRET')]) {
        new File(filePath).withWriter { w ->
            new File(tempFilePath).eachLine { line ->
                w << line.replace("[banzai:${secretConfig.variable}]", SECRET) + System.getProperty("line.separator")
            }
        }
    }
}