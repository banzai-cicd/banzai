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
    def envs = sh(returnStdout: true, script: 'env').split('\n')
    envs.each { name  ->
        println "Name: $name"
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
                    string(name: 'downstreamBuildDefinitions', defaultValue: 'empty', description: 'serialized downstreamBuildDefinitions collection automatically passed during a downstream build chain')
                ])
              ]
            )
        env.GITHUB_API_URL = 'https://github.build.ge.com/api/v3'

        node() { 
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

            sshagent(credentials: config.sshCreds) {
                // TODO notify Flowdock build starting
                echo "My branch is: ${env.BRANCH_NAME}"

                // checkout the branch that triggered the build if not explicitly skipped
                if (config.preCleanup) {
                    logger "Starting Fresh"
                    step([$class: 'WsCleanup'])
                }

                skipSCMStep(config)
                filterSecretsStep(config)
                vulnerabilityScansStep(config)
                buildStep(config)
                publishStep(config)
                deployStep(config)
                integrationTestsStep(config)
                markForPromotionStep(config)
                
                if (config.postCleanup) {
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

        promoteStep(config)
    }
}
