#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {
    if (config.sastBranches) {
      def sastBranchesPattern = config.sastBranches
      Pattern pattern = Pattern.compile(sastBranchesPattern)

      if (!(BRANCH_NAME ==~ pattern)) {
        echo "${BRANCH_NAME} does not match the sastBranches pattern. Skipping SAST"
        return
      }
    }

    // now build, based on the configuration provided
    stage ('SAST Tests') {
      def CHECKMARX_TEAM = /CxServer\\SP\\GE\\GE_PowerWater\\mdi_12782/
      def CHECKMARX_APP = "\\${config.appName}-${env.BRANCH_NAME}"
      def PROJECT_NAME = "${CHECKMARX_TEAM}${CHECKMARX_APP}"

      if (!config.sastCredId) {
        println "SAST: No sastCredId specified: Skipping SAST"
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
              groupId: CHECKMARX_TEAM,
              includeOpenSourceFolders: '',
              jobStatusOnError: 'UNSTABLE',
              password: CHECKMARX_PASSWORD,
              preset: 'Default 2014',
              projectName: PROJECT_NAME,
              serverUrl: 'https://checkmarx.security.ge.com',
              sourceEncoding: '1',
              username: CHECKMARX_USER,
              vulnerabilityThresholdResult: 'FAILURE',
              waitForResultsEnabled: true
        ])

      }

    }
}
