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
    
    withCredentials([file(credentialsId: opts.credId, variable: 'CRED_FILE')]) {
      def credParams = "--on-new-cert trust --auth-key-file ${CRED_FILE}"
      // We have to first check and see if the stream exists since synopsys_coverity step doesn't allow us to react to cmd feedback.
      // 1. check for the existence of the stream 
      def coverityInstallDir = tool name: opts.toolId
      def covManage = "${coverityInstallDir}/bin/cov-manage-im"
      def listStreamsCmd = "unset https_proxy && ${covManage} --mode streams --show --name \"${streamName}\" --host ${opts.serverHost} --port ${opts.serverPort} --ssl ${credParams}"
      def streamList
      try { // have to wrap this because a negative result by cov-manage-im is returned as a shell exit code of 1. awesome TODO, figure out how to get jenkins to ignore this failure in Blue Ocean
        streamList = sh (
          script: listStreamsCmd,
          returnStdout: true,
          returnStatus: false
        ).trim()
      } catch (Exception e) {
        logger "Stream '${streamName}' was not found on the Coverity server"
      }
      
      def addStream = false
      if (!streamList || !streamList.contains(streamName)) {
        addStream = true
      }

      // 2. run the remaining commsnds via synopsys_coverity build step
      // *note* The synopsys_coverity steps provides some variables at runtime to the commands that you specify. Hilariously, the Coverity team decided to use the same ${} syntax that Groovy
      // uses for declaring the Coverity variables. So, we have to build our cov-commit-defects string using a combination of ${groovy} vars and escaped \${coverity} vars      
      def commands = []
      if (addStream) {
        def covAddStreamCmd = "cov-manage-im --mode streams --add --set name:\${COV_STREAM} --set lang:mixed ${credParams} --host \${COVERITY_HOST} --port \${COVERITY_PORT} --ssl"
        def covBindStreamCmd = "cov-manage-im --mode projects --name ${opts.projectName} --update --insert stream:\${COV_STREAM} ${credParams} --host \${COVERITY_HOST} --port \${COVERITY_PORT} --ssl"
        commands.addAll([covAddStreamCmd, covBindStreamCmd])
      }

      def covBuildCmd = "cov-build --dir ${iDir} ${buildCmd}"
      def covAnalyzeCmd = "cov-analyze --dir ${iDir}"
      def covCommitCmd = "cov-commit-defects --dir ${iDir} --stream \${COV_STREAM} ${credParams} --host \${COVERITY_HOST} --https-port \${COVERITY_PORT}"
      commands.addAll([covBuildCmd, covAnalyzeCmd, covCommitCmd])
      
      // convert the list of command strings to a list of command objects
      def formattedCommands = commands.collect { [command: it] }

      synopsys_coverity buildStatusForIssues: 'SUCCESS', 
                changeSetExclusionPatterns: '', 
                changeSetInclusionPatterns: '', 
                checkForIssuesInView: false, 
                commands: formattedCommands, 
                configureChangeSetPatterns: false, 
                coverityAnalysisType: 'COV_ANALYZE', 
                coverityRunConfiguration: 'ADVANCED', 
                coverityToolName: opts.toolId, 
                onCommandFailure: 'SKIP_REMAINING_COMMANDS', 
                projectName: opts.projectName, 
                streamName: streamName, 
                viewName: ''
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