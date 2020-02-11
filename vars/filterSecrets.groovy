#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiFilterSecretsCfg
import com.github.banzaicicd.cfg.BanzaiFilterSecretCfg

void processSecret(BanzaiFilterSecretCfg secretCfg) {

    logger "Filtering Secret: ${secretCfg.secretId}"
    logger secretCfg

    // sanitize the filename
    String file = secretCfg.file
    if (file.contains('..')) {
        error("Secret.file may not contain '..' and should be defined relative to the Jenkins Workspace")
        return
    }

    // copy target file to temp
    String filePath = "${env.WORKSPACE}/${file}"
    // filter and replace deleted original file
    withCredentials([string(credentialsId: secretCfg.secretId, variable: 'SECRET')]) {
        sh "touch ${filePath}"
        String replace = /\[banzai:${secretCfg.label}\]/
        sh "sed -i -e 's/${replace}/${SECRET}/g' ${filePath}"
    }

}

def call(BanzaiFilterSecretsCfg secretsCfg) {

    logger "Filtering Secrets"
    logger secretsCfg
    logger secretsCfg.getClass()
    secretsCfg.secrets.each { secret ->
        BanzaiFilterSecretCfg secretInst = secret
        processSecret(secretInst) }

}