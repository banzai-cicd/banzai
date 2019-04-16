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
    // filter and replace deleted original file
    withCredentials([string(credentialsId: secretConfig.secretId, variable: 'SECRET')]) {
        sh "touch ${filePath}"
        def replace = /\[banzai:${secretConfig.label}\]/
        sh "sed -i -e 's/${replace}/${SECRET}/g' ${filePath}"
    }
}