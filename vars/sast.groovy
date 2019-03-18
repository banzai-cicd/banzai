#!/usr/bin/env groovy

import java.util.regex.Pattern

def call(config) {
    if (config.sastBranches) {
      def sastBranchesPattern = config.sastBranches
      Pattern pattern = Pattern.compile(sastBranchesPattern)

      if (!(BRANCH_NAME ==~ pattern)) {
        logger "${BRANCH_NAME} does not match the sastBranches pattern. Skipping SAST"
        return
      }
    }

    // now build, based on the configuration provided
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
              waitForResultsEnabled: true
        ])

      }

    }
}
