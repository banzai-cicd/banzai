#!/usr/bin/env groovy

def call(config, opts) {
    // TODO: check all required params
    if (!opts.credId) {
        logger "Coverity: No credId specified: Skipping"
        return
    }

    def STREAM_NAME = opts.streamName ?: "${config.appName}-${env.BRANCH_NAME}"
    def buildCmd = env.BUILD_CMD ?: opts.buildCmd
    def iDir = "${env.WORKSPACE}/idir"

    // Clear the intermediate directory if it already exists
    sh "if [ -e ${iDir} ]; then rm -rf ${iDir} ; fi"
    
    withCredentials([file(credentialsId: opts.credId, variable: 'CRED_FILE')]) {
      synopsys_coverity buildStatusForIssues: 'SUCCESS', 
                changeSetExclusionPatterns: '', 
                changeSetInclusionPatterns: '', 
                checkForIssuesInView: false, 
                commands: [
                        [command: "cov-build --dir ${iDir} ${buildCmd}"], 
                        [command: "cov-analyze --dir ${iDir}"], 
                        [command: "cov-commit-defects --dir ${iDir} --host ${COVERITY_HOST} --https-port ${COVERITY_PORT} --stream ${STREAM_NAME} --on-new-cert trust —-auth-key-file ${CRED_FILE}"]], 
                configureChangeSetPatterns: false, 
                coverityAnalysisType: 'COV_ANALYZE', 
                coverityRunConfiguration: 'ADVANCED', 
                coverityToolName: 'default', 
                onCommandFailure: 'SKIP_REMAINING_COMMANDS', 
                projectName: opts.projectName, 
                streamName: STREAM_NAME, 
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