#!/usr/bin/env groovy

def call(config, opts) {
    def PROJECT_NAME = "${config.appName}-${env.BRANCH_NAME}"
    def PRESET = opts.preset ?: '17'
      
    if (!opts.credId) {
      error("credId is required for Checkmarx")
      return
    }
    if (!opts.teamUUID) {
      error("teamUUID is required for Checkmarx")
    }

    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: opts.credId,
                                usernameVariable: 'CHECKMARX_USER', passwordVariable: 'CHECKMARX_PASSWORD']]) {
        step([$class: 'CxScanBuilder',
                comment: '',
                excludeFolders: '', 
                excludeOpenSourceFolders: '',
                filterPattern: '',
                fullScanCycle: 10,
                groupId: "${opts.teamUUID}",
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

    if (opts.resultEmails) {
        opts.resultEmails.each {
                logger "Emailing Checkmarx Scan Results to ${it}"
                emailext attachmentsPattern: '**/ScanReport.pdf', body: "BUILD_URL: ${BUILD_URL}", 
                        subject: "Checkmarx Scan Results: ${env.JOB_NAME} - Build # ${env.BUILD_NUMBER}", 
                        to: it 
                logger "Sent Checkmarx Scan Results..."
        }
        
    }
}