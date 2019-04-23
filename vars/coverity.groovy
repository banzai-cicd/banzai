#!/usr/bin/env groovy

def call(config, opts) {
    def STREAM_NAME = opts.streamName ?: "${config.appName}-${env.BRANCH_NAME}"
    def buildCmd = env.BUILD_CMD ?: opts.buildCmd

    if (!opts.credId) {
        logger "Coverity: No credId specified: Skipping"
        return
    }
    
    withCredentials([file(credentialsId: opts.credId, variable: 'CRED_FILE')]) {
      synopsys_coverity buildStatusForIssues: 'SUCCESS', 
                changeSetExclusionPatterns: '', 
                changeSetInclusionPatterns: '', 
                checkForIssuesInView: false, 
                commands: [
                        [command: "cov-build --dir ${WORKSPACE}/idir ${buildCmd}"], 
                        [command: "cov-analyze --dir ${WORKSPACE}/idir"], 
                        [command: "cov-commit-defects --dir ${WORKSPACE}/idir --host ${COVERITY_HOST} --https-port ${COVERITY_PORT} --stream ${COV_STREAM} —auth-key-file=${CRED_FILE} --on-new-cert trust"]], 
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
                emailext attachmentsPattern: "${WORKSPACE}/idir/output/summary.txt", body: "BUILD_URL: ${BUILD_URL}", 
                        subject: "Coverity Scan Summary: ${env.JOB_NAME} - Build # ${env.BUILD_NUMBER}", 
                        to: it 
                logger "Sent Coverity Scan Results..."
        }
        
    }
}