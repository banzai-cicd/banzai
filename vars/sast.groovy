def call(config) {

    // now build, based on the configuration provided
    stage ('SAST Tests') {
      def CHECKMARX_TEAM = /CxServer\\SP\\GE\\GE_PowerWater\\mdi_12782/
      def CHECKMARX_APP = "\\${config.appName}-${env.BRANCH_NAME}"
      def PROJECT_NAME = "${CHECKMARX_TEAM}${CHECKMARX_APP}"

      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'ge-checkmarx',
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
