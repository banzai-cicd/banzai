#!/usr/bin/env groovy

import hudson.model.User
import com.ge.nola.cfg.BanzaiCfg

@NonCPS
def getRoleBasedUsersList(role) {
  echo "Retrieving users for ${role}..."
  def users = [:]
  def authStrategy = Jenkins.instance.getAuthorizationStrategy()
  if (authStrategy instanceof com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy) {
    def sids = authStrategy.roleMaps.globalRoles.getSidsForRole(role)
    sids.each { sid ->        
      User usr = Jenkins.instance.getUser(sid)
      def usrmail = usr.getProperty(hudson.tasks.Mailer.UserProperty.class)
      if (usrmail.getAddress()) {
          users[sid] = usrmail.getAddress()
      }
      //Jenkins.instance.getUser(sid).fullName
      echo "${sid}: ${usrmail.getAddress()}"
    }
    return users
  } else {
    throw new Exception("Role Strategy Plugin not in use.  Please enable to retrieve users for a role")
  }
}

String[] getUserEmails(users) {
  return users.collect { 
    def mail = Jenkins.instance.getUser(it).getProperty(hudson.tasks.Mailer.UserProperty.class)
    return mail.getAddress()
  }
}

def finalizeDeployment(BanzaiCfg cfg) {
  logger "Finalizing Deployment"
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  String ENV = cfg.internal.gitOps.TARGET_ENV
  String STACK = cfg.internal.gitOps.TARGET_STACK
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

  // 3. commit stack updates
	dir (WORKSPACE) {
		def gitStatus = sh(returnStdout: true, script: 'git status')
		if (!gitStatus.contains('nothing to commit')) {
			sh "git add . && git commit -m 'Updating the following Stack: ${ENV}/${STACK}'"
			sh "git pull && git push origin master"
		} else {
			logger "No new channges to '/${ENV}/${STACK}.yaml' to be commited to the GitOps repository."
		}
	}

  // 4. pass the deployArgs that will get picked up by the Deploy Stage
  cfg.internal.gitOps.DEPLOY_ARGS = [cfg.internal.gitOps.TARGET_ENV, cfg.internal.gitOps.TARGET_STACK]
}

def buildProposedVersionsBody(BanzaiCfg cfg) {
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

  /////
  // if necessary, get approvals
  /////
  def envConfig = cfg.gitOps.envs[ENV]
  String approverEmails
  String approverSSOs
  String watcherEmails
  if (envConfig.approvers || envConfig.watchers) {
    if (envConfig.approvers) {
      approverSSOs = envConfig.approvers.join(",")
      approverEmails = getUserEmails(envConfig.approvers).join(",")
    }
    if (envConfig.watchers) {
      watcherEmails = getUserEmails(envConfig.watchers).join(",")
    }
	} else if (envConfig.approverRole || envConfig.watcherRole) {
    if (envConfig.approverRole) {
      def approverMap = getRoleBasedUsersList(envConfig.approverRole)
      logger "approverMap: ${approverMap.toMapString()}"
      approverEmails = approverMap.values().join(",")
      approverSSOs = approverMap.keySet().join(",")
    }
    if (envConfig.watcherRole) {
      def watcherMap = getRoleBasedUsersList(envConfig.watcherRole)
      logger "watcherMap: ${watcherMap.toMapString()}"
      watcherEmails = watcherMap.values().join(",")
    }
  }

  // IF AND ONLY IF APPROVAL IS REQUIRED, ASK FOR IT
  if (approverEmails && approverSSOs) {
    // notify approvers via email that there is a deployment
    // requested and provide an input step
    stage ("Approve Deployment to '${ENV}'") {
      timeout(time: 3, unit: 'DAYS') {
        def msg = "Deploy to '${ENV}'"
        script {
          try {
            // build approval email
            def proposedServiceVersions = buildProposedVersionsBody(cfg)
            def approvalSubject = "Deployment of the '${STACK}' Stack to the '${ENV}' Environment is requested"
            def approvalMsg = "${approvalSubject} with the following verisions:"
            def approvalBody = "${approvalMsg}\n${proposedServiceVersions}"
            sendEmail to:approverEmails, subject: approvalSubject, body: approvalBody

            // present input steps
            def approverId = input message: msg,
              ok: 'Approve',
              submitter: approverSSOs,
              submitterParameter: 'submitter'

            // build approved email
            String approverName = Jenkins.instance.getUser(approverId).getDisplayName()
            String subject = "Deployment of the '${STACK}' Stack to the '${ENV}' Environment is approved"
            String approvedMsg = "${subject} by ${approverName} with the following versions:"
            String approvedBody = "${approvedMsg}\n${proposedServiceVersions}"
            sendEmail to: [approverEmails, watcherEmails].join(','), subject: subject,
              body: body
            
            // Finalize!
            finalizeDeployment(cfg)
          } catch (err) {
            logger err.message
            String deniedSubject = "Deployment of '${STACK}' to '${ENV}' denied"
            String deniedMsg = "${deniedSubject} by ${err.getCauses()[0].getUser()}"
            logger deniedMsg
            currentBuild.result = 'ABORTED'
            sendEmail to: [approverEmails, watcherEmails].join(','), subject: deniedSubject,
              body: deniedMsg
          }
        }
      }
    } 
  } else {
      logger "No approvals required for deployment"
      finalizeDeployment(cfg)
  }
}