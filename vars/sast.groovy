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

      if (!config.sastCredsId) {
        println "SAST: No sastCredsId specified: Skipping SAST"
        return
      }
      
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: config.sastCredsId,
                                 usernameVariable: 'CHECKMARX_USER', passwordVariable: 'CHECKMARX_PASSWORD']]) {
        sh """#!/bin/bash
          echo Performing /opt/CxConsolePlugin/runCxConsole.sh scan -v    \
          -ProjectName "${PROJECT_NAME}"    \
          -CxServer 'https://checkmarx.security.ge.com'     \
          -CxUser '$CHECKMARX_USER'   \
          -CxPassword '$CHECKMARX_PASSWORD'   \
          -preset 'Default 2014'    \
          -locationtype folder      \
          -locationpath '$WORKSPACE'
          /opt/CxConsolePlugin/runCxConsole.sh scan -v    \
          -ProjectName "${PROJECT_NAME}"    \
          -CxServer 'https://checkmarx.security.ge.com'     \
          -CxUser '$CHECKMARX_USER'   \
          -CxPassword '$CHECKMARX_PASSWORD'   \
          -preset 'Default 2014'    \
          -locationtype folder      \
          -locationpath '$WORKSPACE'
        """
      }

    }
}
