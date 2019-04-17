#!/usr/bin/env groovy

def call(config) {

    stage ('SAST Tests') {
      def PROJECT_NAME = "${config.appName}-${env.BRANCH_NAME}"
      def PRESET = config.sastPreset ?: '17'

      if (!config.sastCredId) {
        logger "SAST: No sastCredId specified: Skipping SAST"
        return
      }
      

      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.sastCredId,
                                 usernameVariable: 'CHECKMARX_USER', passwordVariable: 'CHECKMARX_PASSWORD']]) {

        step([$class: 'CxScanBuilder',
              comment: '',
              excludeFolders: '', 
              excludeOpenSourceFolders: '',
              filterPattern: '',
              fullScanCycle: 10,
              groupId: "${config.sastTeamUUID}",
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
      if (config.sastReportEmailTo) {
          logger "Emailing Checkmarx Scan Results..."
          emailext attachmentsPattern: '**/ScanReport.pdf', body: "BUILD_URL: ${BUILD_URL}", 
                    subject: "Checkmarx Scan Results: ${env.JOB_NAME} - Build # ${env.BUILD_NUMBER}", 
                    to: config.sastReportEmailTo 
          logger "Sent Checkmarx Scan Results..."
      }
    }

}
