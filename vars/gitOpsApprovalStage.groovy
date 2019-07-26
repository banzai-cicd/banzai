#!/usr/bin/env groovy

import hudson.model.User
import com.github.banzaicicd.BanzaiEvent
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiGitOpsInputCfg

@NonCPS
List<String> getRoleBasedUserIds(List<String> roles) {
  logger "Retrieving users for roles '${roles}'"
  def users = []
  def authStrategy = Jenkins.instance.getAuthorizationStrategy()
  if (authStrategy instanceof com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy) {
    roles.each { role ->
      def sids = authStrategy.roleMaps.globalRoles.getSidsForRole(role)
      users = users + sids
    }
  } else {
    throw new Exception("Role Strategy Plugin not in use.  Please enable to retrieve users for a role")
  }

  return users.size() > 0 ? users : null
}

def finalizeDeployment(BanzaiCfg cfg) {
  stage ('Commit Deployment Details') {
    logger "Commit Deployment Details"
    String ENV_DIR_NAME = "${WORKSPACE}/envs"
    String ENV = cfg.internal.gitOps.TARGET_ENV
    String STACK = cfg.internal.gitOps.TARGET_STACK
    String DEPLOYMENT_ID = cfg.internal.gitOps.DEPLOYMENT_ID
    // 1. update the stack yaml with the versions it should be set to
    String stackFileName = "${ENV_DIR_NAME}/${ENV}/${STACK}.yaml"
    def stackYaml = [:]
    try {
      stackYaml = readYaml file: stackFileName
    } catch (e) {
      logger "Creating ${stackFileName} ..."
    }
    
    cfg.internal.gitOps.SERVICE_VERSIONS_TO_UPDATE.each { id, version ->
      logger "Updating Service '${id}' to '${version}' in Stack ${STACK}"
      stackYaml[id] = version
    }
    // 2.  save the updated stack yaml
    sh "rm -rf ${stackFileName}"
    writeYaml file: stackFileName, data: stackYaml

    // 3. save the deployment info in the deployment-history
    String deployHistDir = "${WORKSPACE}/deployment-history/${ENV}/${STACK}"
    sh "mkdir -p ${deployHistDir}" // ensure the dir exists
    sh "rm -rf ${deployHistDir}/${DEPLOYMENT_ID}.yaml" // just incase they give a dup ID...let them
    writeYaml file: "${deployHistDir}/${DEPLOYMENT_ID}.yaml", data: stackYaml

    // 4. commit updates
    dir (WORKSPACE) {
	    
      if (cfg.gitOps.gitUser && cfg.gitOps.gitEmail) {
	      logger "git config user.name '${cfg.gitOps.gitUser}' && git config user.email '${cfg.gitOps.gitEmail}'"
	      sh "git config user.name '${cfg.gitOps.gitUser}' && git config user.email '${cfg.gitOps.gitEmail}'"
      }
	    
      def gitStatus = sh(returnStdout: true, script: 'git status')
      if (!gitStatus.contains('nothing to commit')) {
        sh "git add . && git commit -m 'Updating the following Stack: ${ENV}/${STACK}'"
        sh "git pull && git push origin master"
      } else {
        logger "No new channges to '/${ENV}/${STACK}.yaml' to be commited to the GitOps repository."
      }
    }

    // 5. pass the deployArgs that will get picked up by the Deploy Stage
    cfg.internal.gitOps.DEPLOY_ARGS = [cfg.internal.gitOps.TARGET_ENV, cfg.internal.gitOps.TARGET_STACK]
  }
}

def buildProposedVersionsString(BanzaiCfg cfg) {
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  String ENV = cfg.internal.gitOps.TARGET_ENV
  String STACK = cfg.internal.gitOps.TARGET_STACK
  // include full proposed stack versions (not just services being updated)
  String stackFileName = "${ENV_DIR_NAME}/${ENV}/${STACK}.yaml"
  def stackYaml = [:]
  try {
    readYaml file: stackFileName
  } catch (e) {
    // the stack yaml my not yet exist if this is part of an environment promotion
    logger "${stackFileName} does not yet exist. Will create if approved"
  }
  cfg.internal.gitOps.SERVICE_VERSIONS_TO_UPDATE.each { serviceId, version ->
    stackYaml[serviceId] = version
  }
  def formatedStack = stackYaml.collect { "${it.key} : ${it.value}" }

  return formatedStack.join('\n')
}

def call(BanzaiCfg cfg) {
  /////
  // This Stage will always run for a GitOps repo when cfg.internal.gitOps.DEPLOY = true
  /////
  if (!cfg.gitOps || !cfg.internal.gitOps.DEPLOY) {
      logger "Does not qualify for 'GitOps: Deployment Approval Stage'"
      return
  }
  String ENV = cfg.internal.gitOps.TARGET_ENV
  String STACK = cfg.internal.gitOps.TARGET_STACK

  // see if we have any required approvers
  def envConfig = cfg.gitOps.envs[ENV]
  def approverIds
  if (envConfig.approverIds) {
    approverIds = envConfig.approverIds
	} else if (envConfig.approverRoles) { // requires role
    approverIds = getRoleBasedUserIds(envConfig.approverRoles)
  }

  // IF AND ONLY IF APPROVAL IS REQUIRED, ASK FOR IT
  if (approverIds != null) {
    // 1. notify approvers
    // 2. request and provide an input step
    BanzaiGitOpsInputCfg inputCfg = cfg.gitOps.inputCfg ?: new BanzaiGitOpsInputCfg()
    String stageName = "Approve Deployment to '${ENV}'"
    stage (stageName) {
      // build approval notification
      String serviceVersions = buildProposedVersionsString(cfg)
      String approvalMsg =
      """
      Deployment of the '${STACK}' Stack to the '${ENV}' Environment is requested with the following verisions:
      ${serviceVersions}
      """.stripMargin().stripIndent()
      notify(cfg, [
          scope: BanzaiEvent.Scope.GITOPS,
          status: BanzaiEvent.Status.APPROVAL,
          stage: stageName,
          message: approvalMsg
      ])

      timeout(time: inputCfg.approvalTimeoutDays, unit: 'DAYS') {
        script {
          try {
            // present input steps
            def approverId = input message: "Deploy to '${ENV}'",
              ok: 'Approve',
              submitter: approverIds.join(','),
              submitterParameter: 'submitter'

            // build approved notification
            String approverName = Jenkins.instance.getUser(approverId).getDisplayName()
            String approvedMsg =
            """
            Deployment of the '${STACK}' Stack to the '${ENV}' Environment approved by ${approverName} with the following versions:
            ${serviceVersions}
            """.stripMargin().stripIndent()
            notify(cfg, [
                scope: BanzaiEvent.Scope.GITOPS,
                status: BanzaiEvent.Status.APPROVED,
                stage: stageName,
                message: approvedMsg\
            ])
            
            // Finalize!
            finalizeDeployment(cfg)
          } catch (err) {
            logger err.message
            String deniedMsg = 
            """
            Deployment of '${STACK}' to '${ENV}' denied by ${err.getCauses()[0].getUser()}"
            """.stripMargin().stripIndent()

            logger deniedMsg
            currentBuild.result = 'ABORTED'
            notify(cfg, [
                scope: BanzaiEvent.Scope.GITOPS,
                status: BanzaiEvent.Status.ABORTED,
                stage: stageName,
                message: deniedMsg
            ])

            throw err
          }
        }
      }
    } 
  } else {
      logger "No approvals required for deployment"
      finalizeDeployment(cfg)
  }
}
