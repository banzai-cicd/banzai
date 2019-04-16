#!/usr/bin/env groovy
import java.nio.file.Files
import java.io.File

def call(secretConfig) {
    logger "Filtering Secret: ${secretConfig.secretId}"

    // sanitize the filename
    def file = secretConfig.file
    if (file.contains('..')) {
        error("Secret.file may not contain '..' and should be defined relative to the Jenkins Workspace")
        return
    }

    // copy target file to temp
    def filePath = File.join(env.WORKSPACE, file)
    def tempFilePath = File.join(env.WORKSPACE, "${file}.temp")
    Files.copy(filePath, tempFilePath)
    Files.delete(filePath)

    // filter and replace deleted original file
    withCredentials([string(credentialsId: config[secretConfig.secretId], variable: 'SECRET')]) {
        new File(filePath).withWriter { w ->
            new File(tempFilePath).eachLine { line ->
                w << line.replace("[banzai:${secretConfig.variable}]", SECRET) + System.getProperty("line.separator")
            }
        }
    }
}