def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // now build, based on the configuration provided
    node {
      stage ('SAST Tests') {

        // checkout the branch that triggered the build
        checkout scm

        def CHECKMARX_TEAM = /CxServer\\SP\\GE\\GE_PowerWater\\mdi_12782/
        def CHECKMARX_APP = "\\${config.appName}-${env.BRANCH_NAME}"
        def PROJECT_NAME = "${CHECKMARX_TEAM}${CHECKMARX_APP}"
        // checkout([$class: 'GitSCM', branches: [[name: "${branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'MessageExclusion', excludedMessage: '.*\\[skip-ci\\].*'], [$class: 'LocalBranch', localBranch: "${branch}"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'ge-git', url: "${config.cloneAppUrl}"]]])

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

        currentBuild.result = 'SUCCESS'
      }

    }
}
