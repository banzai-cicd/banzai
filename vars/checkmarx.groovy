#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiVulnerabilityCfg

def call(BanzaiCfg config, vulnerabilityCfg) {
    def PROJECT_NAME = "${config.appName}-${env.BRANCH_NAME}"
    def PRESET = vulnerabilityCfg.preset ?: '17'
      
    if (!vulnerabilityCfg.credId) {
      error("credId is required for Checkmarx")
      return
    }
    if (!vulnerabilityCfg.teamUUID) {
      error("teamUUID is required for Checkmarx")
      return
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: vulnerabilityCfg.credId,
                                usernameVariable: 'CHECKMARX_USER', passwordVariable: 'CHECKMARX_PASSWORD']]) {
        step([$class: 'CxScanBuilder',
                comment: '',
                excludeFolders: '', 
                excludeOpenSourceFolders: '',
                filterPattern: '',
                fullScanCycle: 10,
                groupId: "${vulnerabilityCfg.teamUUID}",
                includeOpenSourceFolders: '',
                jobStatusOnError: 'UNSTABLE',
                password: "${CHECKMARX_PASSWORD}",
                preset: PRESET,
                projectName: "${PROJECT_NAME}",
                serverUrl: 'https://my-checkmarx.com',
                sourceEncoding: '1',
                username: "${CHECKMARX_USER}",
                vulnerabilityThresholdResult: 'FAILURE',
                waitForResultsEnabled: true,
                generatePdfReport: true
        ])
    }
}