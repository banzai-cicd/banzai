#!/usr/bin/env groovy

def call(config, opts) {
    def streamName = opts.streamName ?: "${config.appName}_${env.BRANCH_NAME}"
    def buildCmd = env.BUILD_CMD ?: opts.buildCmd // accept build command if set in the environment from a previous step.
    def iDir = "${env.WORKSPACE}/idir"
    
    // TODO: check all required params
    if (!opts.credId) {
        error("credId is required for Coverity")
        return
    }
    if (!opts.projectName) {
        error("projectName is required for Coverity")
        return
    }
    if (!buildCmd) {
        error("buildCmd or env.BUILD_CMD is required for Coverity")
        return
    }

    // Clear the intermediate directory if it already exists
    sh "if [ -e ${iDir} ]; then rm -rf ${iDir} ; fi"
    
    withCredentials([file(credentialsId: opts.credId, variable: 'CRED_FILE')]) {
      
      // The Coverity plugin provides some variables at runtime to the commands that you specify. Hilariously, the Coverity team decided to use the same ${} syntax that Groovy
      // uses for declaring the Coverity variables. So, we have to build our cov-commit-defects string using a combination of ${groovy} vars and escaped \${coverity} vars
      def hostAndPort = "--host \${COVERITY_HOST} --https-port \${COVERITY_PORT}"
      def credParams = "--on-new-cert trust --auth-key-file ${CRED_FILE}"
      def covManageCmd = "cov-manage-im --mode stream --add --set name:\${COV_STREAM} --set lang:mixed ${credParams} ${hostAndPort}"
      def covBuildCmd = "cov-build --dir ${iDir} ${buildCmd}"
      def covAnalyzeCmd = "cov-analyze --dir ${iDir}"
      def covCommitCmd = "cov-commit-defects --dir ${iDir} --stream \${COV_STREAM} ${credParams} ${hostAndPort}"

      synopsys_coverity buildStatusForIssues: 'SUCCESS', 
                changeSetExclusionPatterns: '', 
                changeSetInclusionPatterns: '', 
                checkForIssuesInView: false, 
                commands: [
                        [command: covManageCmd],
                        [command: covBuildCmd], 
                        [command: covAnalyzeCmd], 
                        [command: covCommitCmd]], 
                configureChangeSetPatterns: false, 
                coverityAnalysisType: 'COV_ANALYZE', 
                coverityRunConfiguration: 'ADVANCED', 
                coverityToolName: 'default', 
                onCommandFailure: 'SKIP_REMAINING_COMMANDS', 
                projectName: opts.projectName, 
                streamName: streamName, 
                viewName: ''
    }

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