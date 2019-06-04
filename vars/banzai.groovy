#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg

def call(cfgMap) {
    // evaluate the body block, and collect configuration into the object
    def cfg = new BanzaiCfg(cfgMap)

    if (cfg.throttle) {
        throttle(cfg.throttle.tokenize(',')) {
            runPipeline(cfg)
        }
    } else {
        runPipeline(cfg)
    }
}

def printEnv() {
    logger "Printing Available Environment Variables"
    def envs = sh(returnStdout: true, script: 'env').split('\n')
    envs.each { name  ->
        logger "Name: $name"
    }
}

def runPipeline(BanzaiCfg cfg) {
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
            setProxy(cfg)

            // support for jenkins 'tools'
            if (cfg.jdk) {
                jdk = tool name: cfg.jdk
                env.JAVA_HOME = "${jdk}"

                logger "JAVA_HOME: ${jdk}"
            }

            if (cfg.nodejs) {
                def nodeVersion = "node ${cfg.nodejs}"
                env.NODEJS_HOME = "${tool nodeVersion}"
                // on linux / mac
                env.PATH = "${env.NODEJS_HOME}/bin:${env.PATH}"
            }
            
            if (!cfg.sshCreds) {
                cfg.sshCreds = []
            }
            sshagent(credentials: cfg.sshCreds) {
                // TODO notify Flowdock build starting
                echo "My branch is: ${env.BRANCH_NAME}"

                // checkout the branch that triggered the build if not explicitly skipped
                if (cfg.preCleanWorkspace) {
                    logger "Cleaning Workspace"
                    step([$class: 'WsCleanup'])
                }
                
                scmStage(cfg)
                powerDevOpsInitReportingSettings(cfg)
                filterSecretsStage(cfg)
                // gitOpsStages
                gitOpsUpdateServiceVersionsStage(cfg)
                gitOpsUserInputStages(cfg)
                gitOpsApprovalStage(cfg)
                // /end gitOpsStages
                scansStage(cfg, 'vulnerability')
                scansStage(cfg, 'quality')
                buildStage(cfg)
                publishStage(cfg)
                deployStage(cfg)
                gitOpsTriggerStage(cfg)
                integrationTestsStage(cfg)
                powerDevOpsReportingStage(cfg)

                if (cfg.postCleanWorkspace) {
                    logger "Cleaning Workspace"
                    step([$class: 'WsCleanup'])
                }

                if (cfg.downstreamBuilds || params.downstreamBuildIds != 'empty') {
                    downstreamBuilds(cfg)
                }

                currentBuild.result = 'SUCCESS'
                notify(cfg, 'Pipeline', 'All Stages Complete', 'SUCCESS')
            } // ssh-agent
        } // node
    }
}
