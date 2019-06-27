#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiFilterSecretsCfg

def call(BanzaiFilterSecretsCfg secretsCfg) {

    logger "Filtering Secret: ${secretsCfg.secretId}"
    logger secretsCfg

    // sanitize the filename
    String file = secretsCfg.file
    if (file.contains('..')) {
        error("Secret.file may not contain '..' and should be defined relative to the Jenkins Workspace")
        return
    }

    // copy target file to temp
    String filePath = "${env.WORKSPACE}/${file}"
    // filter and replace deleted original file
    withCredentials([string(credentialsId: secretsCfg.secretId, variable: 'SECRET')]) {
        sh "touch ${filePath}"
        String replace = /\[banzai:${secretsCfg.label}\]/
        sh "sed -i -e 's/${replace}/${SECRET}/g' ${filePath}"
    }

}