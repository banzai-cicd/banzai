#!/usr/bin/env groovy

def call(config, opts) {
    def streamName = opts.streamName ?: "${config.appName}_${env.BRANCH_NAME}"
    def buildCmd = env.BUILD_CMD ?: opts.buildCmd // accept build command if set in the environment from a previous step.
    def iDir = "${env.WORKSPACE}/idir"
    
    // check for all required opts
    def requiredOpts = ['serverHost', 'serverPort', 'toolId', 'credId', 'projectName']
    def failedRequiredOpts = requiredOpts.findAll { !opts[it] }
    if (!buildCmd) {
        failedRequiredOpts.add('buildCmd or env.BUILD_CMD')
    }
    if (failedRequiredOpts.size() > 0) {
        def isOrAre = failedRequiredOpts.size() > 1 ? 'are' : 'is'
        error("${failedRequiredOpts.join(', ')} ${isOrAre} required for Coverity")
        return
    }

    // Clear the intermediate directory if it already exists from a previous run
    sh "if [ -e ${iDir} ]; then rm -rf ${iDir} ; fi"

    // wrap Coverity Env
    withCoverityEnvironment(coverityInstanceUrl: 'https://coverity.power.ge.com:443', projectName: opts.projectName, streamName: streamName, viewName: '') {
      withCredentials([file(credentialsId: opts.credId, variable: 'CRED_FILE')]) {
        def credParams = "--on-new-cert trust --auth-key-file ${CRED_FILE}"
        // We have to first check and see if the stream exists since synopsys_coverity step doesn't allow us to react to cmd feedback.
        // 1. check for the existence of the stream 
        def listStreamsCmd = "unset https_proxy && cov-manage-im --mode streams --show --name ${COV_STREAM} --url ${COV_URL} --ssl ${credParams} | grep ${COV_STREAM}"
        def streamList
        try { // have to wrap this because a negative result by cov-manage-im is returned as a shell exit code of 1. awesome TODO, figure out how to get jenkins to ignore this failure in Blue Ocean
          streamList = sh (
            script: listStreamsCmd,
            returnStdout: true,
            returnStatus: false
          ).trim()
        } catch (Throwable e) {
          logger "Stream '${streamName}' was not found on the Coverity server"
          logger e.getStackTrace()
          logger streamList
          return
        }
        
        def addStream = false
        if (!streamList || !streamList.contains(streamName)) {
          addStream = true
        }

        // 2. run the remaining commsnds 
        def commands = []
        if (addStream) {
          def covAddStreamCmd = "cov-manage-im --mode streams --add --set name:${COV_STREAM} --set lang:mixed ${credParams} --url ${COV_URL} --ssl"
          def covBindStreamCmd = "cov-manage-im --mode projects --name ${COV_PROJECT} --update --insert stream:${COV_STREAM} ${credParams} --url ${COV_URL} --ssl"
          commands.addAll([covAddStreamCmd, covBindStreamCmd])
        }

        def covBuildCmd = "cov-build --dir ${iDir} ${buildCmd}"
        def covAnalyzeCmd = "cov-analyze --dir ${iDir}"
        def covCommitCmd = "cov-commit-defects --dir ${iDir} --stream ${COV_STREAM} ${credParams} --url ${COV_URL}"
        commands.addAll([covBuildCmd, covAnalyzeCmd, covCommitCmd])
        
        // run each command
        def stdOut
        try {
          commands.each {
            stdOut = sh (
              script: it,
              returnStdout: true,
              returnStatus: false
            ).trim()
          }
        } catch (Throwable e) {
          logger e.getStackTrace()
          logger stdOut
        }
        
      } // with
    }

    // email the summary.txt if applicable
    if (opts.resultEmails) {
        opts.resultEmails.each {
                logger "Emailing Coverity Scan Results to ${it}"
                emailext attachmentsPattern: "**/idir/output/summary.txt", body: "BUILD_URL: ${env.BUILD_URL}", 
                        subject: "Coverity Scan Summary: ${env.JOB_NAME} - Build # ${env.BUILD_NUMBER}", 
                        to: it 
                logger "Sent Coverity Scan Results..."
        }
        
    }
}