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

        /*
        Determine the total number of steps in the pipeline that are activated
        Jenkins Pipelines don't allow many groovy methods (CPS issues) like .findAll...hence the nastiness
        */
        def steps = []
        for (entry in [
                !config.skipSCM,
                config.sast, 
                config.build, 
                config.publish, 
                config.deploy, 
                config.integrationTests,
                config.markForPromotion, 
                config.promote, 
                config.downstreamBuilds
            ]) {
            if (entry == true) {
                steps.push(entry)
            }
        }
        def passedSteps = 0

        def passStep = { step ->
            passedSteps += 1
            logger "BANZAI: ${step} PASSED : ${passedSteps}/${steps.size()} STEPS COMPLETE"
            if (passedSteps >= steps.size()) {
                currentBuild.result = 'SUCCESS'
            }
        }

        def isGithubError = { err ->
            return err.message.contains("The suplied credentials are invalid to login") ? true : false
        }

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

            // support for filtering files and inserting Jenkins Secrets
            if (config.secretFilters) {
                config.secretFilters.each {
                    filterSecret(it)
                }
            }

            sshagent(credentials: config.sshCreds) {
                // TODO notify Flowdock build starting
                echo "My branch is: ${env.BRANCH_NAME}"

                // checkout the branch that triggered the build if not explicitly skipped
                if (config.preCleanup) {
                    logger "Starting Fresh"
                    step([$class: 'WsCleanup'])
                }

                if (!config.skipSCM) {
                    try {
                        notify(config, 'Checkout', 'Pending', 'PENDING')
                        checkoutSCM(config)
                        passStep('CHECKOUT')
                        notify(config, 'Checkout', 'Successful', 'SUCCESS')
                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'UNSTABLE'
                        if (isGithubError(err)) {
                            notify(config, 'Checkout', 'githubdown', 'FAILURE', true)
                        } else {
                            notify(config, 'Checkout', 'Failed', 'FAILURE')
                        }
                        throw err
                    }
                }

                if (config.sast) {
                    try {
                        notify(config, 'SAST', 'Pending', 'PENDING')
                        sast(config)
                        passStep('SAST')
                        notify(config, 'SAST', 'Successful', 'SUCCESS')
                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'UNSTABLE'
                        notify(config, 'Build', 'Failed', 'FAILURE')
                        throw err
                    }
                }

                if (config.build) {
                    try {
                        notify(config, 'Build', 'Pending', 'PENDING')
                        banzaiBuild(config)
                        passStep('BUILD')
                        notify(config, 'Build', 'Successful', 'SUCCESS')
                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'FAILURE'
                        if (isGithubError(err)) {
                            notify(config, 'Build', 'githubdown', 'FAILURE', true)
                        } else {
                            notify(config, 'Build', 'Failed', 'FAILURE')
                        }
                        throw err
                    }
                }

                /*
                all notify calls past the build stage will skip notifcations to github
                */
                if (config.publish) {
                    try {
                        notify(config, 'Publish', 'Pending', 'PENDING', true)
                        publish(config)
                        passStep('PUBLISH')
                        notify(config, 'Publish', 'Successful', 'SUCCESS', true)
                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'FAILURE'
                        if (isGithubError(err)) {
                            notify(config, 'Publish', 'githubdown', 'FAILURE', true)
                        } else {
                            notify(config, 'Publish', 'Failed', 'FAILURE', true)
                        }
                        throw err
                    }
                }

                if (config.deploy) {
                    try {
                        notify(config, 'Deploy', 'Pending', 'PENDING', true)
                        deploy(config)
                        passStep('DEPLOY')
                        notify(config, 'Deploy', 'Successful', 'SUCCESS', true)
                        // TODO notify Flowdock
                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'FAILURE'
                        notify(config, 'Deploy', 'Failed', 'FAILURE', true)
                        throw err
                    }
                }

                if (config.integrationTests) {
                    try {
                        notify(config, 'IT', 'Pending', 'PENDING', true)

                        if (config.xvfb) {
                            def screen = config.xvfbScreen ?: '1800x900x24'

                            wrap([$class: 'Xvfb', screen: screen]) {
                                integrationTests(config)
                            }
                        } else {
                            integrationTests(config)
                        }

                        passStep('IT')
                        notify(config, 'IT', 'Successful', 'SUCCESS', true)
                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'FAILURE'
                        notify(config, 'IT', 'Failed', 'FAILURE', true)
                        throw err
                    }
                }

                if (config.markForPromotion) {
                    try {
                        notify(config, 'MarkForPromotion', 'Pending', 'PENDING', true)
                        markForPromotion(config)
                        passStep('MARK FOR PROMOTION')
                        notify(config, 'MarkForPromotion', 'Successful', 'SUCCESS', true)
                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'FAILURE'
                        notify(config, 'MarkForPromotion', 'Failed', 'FAILURE', true)
                        throw err
                    }
                }

                if (config.postCleanup) {
                    logger "Cleaning up"
                    step([$class: 'WsCleanup'])
                }

                if (config.downstreamBuilds || params.downstreamBuildIds != 'empty') {
                    downstreamBuilds(config)
                }
            } // ssh-agent
        } // node

        if (config.promote) {
            try {
                notify(config, 'Promote', 'Pending', 'PENDING', true)
                promote(config)
                passStep('PROMOTE')
                notify(config, 'Promote', 'Successful', 'SUCCESS', true)
                // TODO notify Flowdock
            } catch (err) {
                echo "Caught: ${err}"
                currentBuild.result = 'FAILURE'
                notify(config, 'Promote', 'Failed', 'FAILURE', true)
                throw err
            }
        }
    }
}
