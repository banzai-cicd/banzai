#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiVulnerabilityCfg

def call(BanzaiCfg config, vulnerabilityCfg) {
    def PROJECT_NAME = "${config.appName}-${env.BRANCH_NAME}"
    def PRESET = vulnerabilityCfg.preset ?: '17'
      
    if (!vulnerabilityCfg.credId) {
      error("credId is required for Checkmarx")
      return
    }
    if (!vulnerabilityCfg.teamUUID) {
      error("teamUUID is required for Checkmarx")
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
                serverUrl: 'https://checkmarx.security.ge.com',
                sourceEncoding: '1',
                username: "${CHECKMARX_USER}",
                vulnerabilityThresholdResult: 'FAILURE',
                waitForResultsEnabled: true,
                generatePdfReport: true
        ])
    }

    if (config.powerDevOpsReporting) {
        checkmarxSastResults()
    }

    if (vulnerabilityCfg.resultEmails) {
        vulnerabilityCfg.resultEmails.each {
          sendEmail(it, "Checkmarx Scan Results: ${env.JOB_NAME}", "BUILD_URL: ${BUILD_URL}",
            '**/ScanReport.pdf')
        }
    }
}