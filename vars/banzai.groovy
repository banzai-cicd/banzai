#!/usr/bin/env groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.throttle) {
        throttle(config.throttle.tokenize(',')) {
            runPipeline(config)
        }
    } else {
        runPipeline(config)
    }
}

def printEnv() {
    logger "Printing Available Environment Variables"
    def envs = sh(returnStdout: true, script: 'env').split('\n')
    envs.each { name  ->
        logger "Name: $name"
    }
}

def runPipeline(config) {
    pipeline {
        // clean up old builds (experimental, not sure if this is actually working or not. time will tell)
        properties(
            [
                buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '5', numToKeepStr: '10')),
                parameters([
                    string(name: 'downstreamBuildIds', defaultValue: 'empty', description: 'list of buildIds to execute against'), 
                    string(name: 'downstreamBuildDefinitions', defaultValue: 'empty', description: 'serialized downstreamBuildDefinitions collection automatically passed during a downstream build chain'),
                    string(name: 'gitOpsTriggeringBranch', defaultValue: 'empty', description: 'The BRANCH_NAME if the pipeline was triggered via the gitOpsTrigger in an upstream build'),
                    string(name: 'gitOpsVersions', defaultValue: 'empty', description: "An object of 'serviceId' and 'version' pairs that should be updated in the gitOps repo of a given project"),
                    string(name: 'gitOpsStackId', defaultValue: 'empty', description: 'The id of the stack which the triggering service belongs to.')
                ])
            ]
        )
        env.GITHUB_API_URL = 'https://github.build.ge.com/api/v3'

        node() {
            printEnv()
            
            // ensure proxy fields are properly set
            setProxy(config)

            // support for jenkins 'tools'
            if (config.jdk) {
                jdk = tool name: config.jdk
                env.JAVA_HOME = "${jdk}"

                logger "JAVA_HOME: ${jdk}"
            }

            if (config.node) {
                def nodeVersion = "node ${config.node}"
                env.NODEJS_HOME = "${tool nodeVersion}"
                // on linux / mac
                env.PATH = "${env.NODEJS_HOME}/bin:${env.PATH}"
            }
            
            if (!config.sshCreds) {
                config.sshCreds = []
            }
            sshagent(credentials: config.sshCreds) {
                // TODO notify Flowdock build starting
                echo "My branch is: ${env.BRANCH_NAME}"

                // checkout the branch that triggered the build if not explicitly skipped
                if (config.preCleanWorkspace) {
                    logger "Starting Fresh"
                    step([$class: 'WsCleanup'])
                }
                
                scmStage(config)
                powerDevOpsInitReportingSettings(config)
                filterSecretsStage(config)
                // gitOpsStages
                gitOpsUpdateServiceVersionsStage(config)
                gitOpsUserInputStages(config)
                gitOpsApprovalStage(config)
                // /end gitOpsStages
                scansStage(config, 'vulnerability')
                scansStage(config, 'quality')
                buildStage(config)
                publishStage(config)
                deployStage(config)
                gitOpsTriggerStage(config)
                integrationTestsStage(config)
                powerDevOpsReportingStage(config)
                markForPromotionStage(config)

                if (config.postCleanWorkspace) {
                    logger "Cleaning up"
                    step([$class: 'WsCleanup'])
                }

                if (config.downstreamBuilds || params.downstreamBuildIds != 'empty') {
                    downstreamBuilds(config)
                }

                currentBuild.result = 'SUCCESS'
                notify(config, 'Pipeline', 'All Stages Complete', 'SUCCESS')
            } // ssh-agent
        } // node

        promoteStage(config)
    }
}
