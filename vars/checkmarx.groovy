#!/usr/bin/env groovy

def call(config, opts) {
    def PROJECT_NAME = "${config.appName}-${env.BRANCH_NAME}"
    def PRESET = opts.preset ?: '17'
      
    if (!opts.credId) {
    logger "Checkmarx: No credId specified: Skipping"
    return
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
    if (opts.resultEmail) {
        logger "Emailing Checkmarx Scan Results..."
        emailext attachmentsPattern: '**/ScanReport.pdf', body: "BUILD_URL: ${BUILD_URL}", 
                subject: "Checkmarx Scan Results: ${env.JOB_NAME} - Build # ${env.BUILD_NUMBER}", 
                to: opts.resultEmail 
        logger "Sent Checkmarx Scan Results..."
    }
}