#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiStageCfg
import com.ge.nola.BanzaiEvent

def call(cfgMap) {
    // evaluate the body block, and collect configuration into the object
    def cfg = new BanzaiCfg(cfgMap)

    if (cfg.throttle) {
        throttle(cfg.throttle) {
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
                    string(name: 'downstreamBuildCfgs', defaultValue: 'empty', description: 'serialized downstreamBuildCfgs collection automatically passed during a downstream build chain'),
                    string(name: 'gitOpsTriggeringBranch', defaultValue: 'empty', description: 'The BRANCH_NAME if the pipeline was triggered via the gitOpsTrigger in an upstream build'),
                    string(name: 'gitOpsVersions', defaultValue: 'empty', description: "An object of 'serviceId' and 'version' pairs that should be updated in the gitOps repo of a given project"),
                    string(name: 'gitOpsStackId', defaultValue: 'empty', description: 'The id of the stack which the triggering service belongs to.')
                ])
            ]
        )

        timeout (cfg.timeout) {
            node() {
                try {
                    printEnv()

                    // ensure proxy fields are properly set
                    setProxy(cfg)

                    // support for jenkins 'tools'
                    if (cfg.tools) {
                        if (cfg.tools.jdk) {
                            def jdk = tool name: cfg.tools.jdk
                            env.JAVA_HOME = "${jdk}"

                            logger "JAVA_HOME: ${jdk}"
                        }

                        if (cfg.tools.nodejs) {
                            def nodeTool = tool name: cfg.tools.nodejs
                            env.NODEJS_HOME = "${nodeTool}"
                            // on linux / mac
                            env.PATH = "${env.NODEJS_HOME}/bin:${env.PATH}"
                        }
                    }

                    sshagent(credentials: cfg.sshCreds) {
                        notify(cfg, [
                            scope: BanzaiEvent.Scope.PIPELINE,
                            status: BanzaiEvent.Status.PENDING,
                            message: 'Pipeline pending...'
                        ])
                        // TODO notify Flowdock build starting
                        logger "My branch is: ${env.BRANCH_NAME}"

                        // checkout the branch that triggered the build if not explicitly skipped
                        if (cfg.cleanWorkspace?.pre? != null) {
                            cleanWorkspace(cfg)
                        }
                        
                        scmStage(cfg)
                        if (cfg.hooks?.stages?.pre? != null) {
                            // call pre-stages-run hook
                            cfg.hooks.stages.pre(cfg)
                        }
                        //powerDevOpsInitReportingSettings(cfg)
                        filterSecretsStage(cfg)
                        // gitOps input stages
                        gitOpsUpdateServiceVersionsStage(cfg)
                        gitOpsUserInputStages(cfg)
                        gitOpsApprovalStage(cfg)
                        // project-provided pipeline stages
                        if (cfg.stages) {
                            logger "Executing Custom Banzai Stages"
                            cfg.stages.each { BanzaiStageCfg stage ->
                                if (stage.isBanzaiStage()) {
                                    List<String> parts = stage.name.tokenize(':')
                                    String stageName = parts.removeAt(0)
                                    def args = [cfg] + parts
                                    /*
                                        jenkins doesn't support the spread operator so I can't do
                                        this."${stageName}Stage"(*args)
                                        which would be a nice one-liner for supporting stages w/ variable args
                                        ugggghhhhhhhhhhh. TODO: revisit with consistent args object
                                    */
                                    if (stageName == 'scans') {
                                        "${stageName}Stage"(args[0], args[1])
                                    } else {
                                        "${stageName}Stage"(args[0])
                                    }
                                } else {
                                    customBanzaiStage(cfg, stage)
                                }
                            }
                        } else {
                            buildStage(cfg)
                            scansStage(cfg, 'vulnerability')
                            scansStage(cfg, 'quality')
                            publishStage(cfg)
                            deployStage(cfg)
                            integrationTestsStage(cfg)
                        }

                        // gitOps trigger stage
                        gitOpsTriggerStage(cfg)
                        // report results to power devOps
                        if (cfg.hooks?.stages?.post? != null) {
                            // call post-stages-run hook
                            cfg.hooks.stages.post(cfg)
                        }
                        //powerDevOpsReportingStage(cfg)

                        if (cfg.downstreamBuilds || params.downstreamBuildIds != 'empty') {
                            downstreamBuilds(cfg)
                        }
                    } // ssh-agent
                } catch (Exception e) {
                    logger "Pipeline FAILED"
                    currentBuild.result = "${BanzaiEvent.Status.FAILURE}"
                    notify(cfg, [
                        scope: BanzaiEvent.Scope.PIPELINE,
                        status: BanzaiEvent.Status.FAILURE,
                        message: 'Error During Pipeline Execution'
                    ])

                    throw e
                } finally { // ensure cleanup is performed if configured
                    if (cfg.cleanWorkspace?.post? != null) {
                        cleanWorkspace(cfg)
                    }
                }

                currentBuild.result = "${BanzaiEvent.Status.SUCCESS}"
                notify(cfg, [
                    scope: BanzaiEvent.Scope.PIPELINE,
                    status: BanzaiEvent.Status.SUCCESS,
                    message: 'All Stages Complete'
                ])
                return
            } // node
        }
    }
}
