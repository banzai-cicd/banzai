#!/usr/bin/env groovy

import hudson.model.User

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

def finalizeDeployment(config) {
  logger "Finalizing Deployment"
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  String ENV = config.gitOps.TARGET_ENV
  String STACK = config.gitOps.TARGET_STACK
  // 1. update the stack yaml with the versions it should be set to
  String stackFileName = "${ENV_DIR_NAME}/${ENV}/${STACK}.yaml"
	def stackYaml = readYaml file: stackFileName
  config.gitOps.STACK_VERSIONS_TO_UPDATE.each { id, version ->
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
			logger "No new Services versions commited to the GitOps repository."
		}
	}

  // 4. pass the deployArgs that will get picked up by the Deploy Stage
  config.deployArgs = [config.gitOps.TARGET_ENV, config.gitOps.TARGET_STACK]
}

def call(config) {
  String SERVICE_DIR_NAME = "${WORKSPACE}/services"
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  /////
  // This Stage will always run for a GitOps repo when config.deploy = true
  /////
  if (!config.gitOps || !config.deploy) {
      logger "Does not qualify for 'GitOps: Deployment Approval Stage'"
      return
  }
  String TARGET_ENV = config.gitOps.TARGET_ENV
  String TARGET_STACK = config.gitOps.TARGET_STACK

  /////
  // if necessary, get approvals
  /////
  def envConfig = config.gitOps.envs[targetEnvironment]
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
    stage ("Approve Deployment to '${TARGET_ENV}'") {
      timeout(time: 3, unit: 'DAYS') {
        def msg = "Deploy to '${TARGET_ENV}'"
        script {
          try {
            def approverId = input message: msg,
              ok: 'Approve',
              submitter: approverSSOs,
              submitterParameter: 'submitter'

            String approverName = Jenkins.instance.getUser(approverId).getDisplayName()
            String subject = "Deployment of '${TARGET_STACK}' Stack to '${TARGET_ENV}' Environment approved"
            String approvedMsg = "${subject} by ${approverName} with the following versions:"
            def versionKVs = versions.collect { "${it.key} : ${it.value}" }
            String body = "${approvedMsg}\n${versionKVs.join('\n')}"
            gitOpsSendEmail(approverEmails, watcherEmails, subject, body)

            finalizeDeployment(config)
          } catch (err) {
            logger err.message
            String deniedSubject = "Deployment of '${TARGET_STACK}' to '${TARGET_ENV}' denied"
            String deniedMsg = "${deniedSubject} by ${err.getCauses()[0].getUser()}"
            logger deniedMsg
            currentBuild.result = 'ABORTED'
            gitOpsSendEmail(approverEmails, watcherEmails, deniedSubject, deniedMsg)
          }
        }
      }
    } 
  } else {
      logger "No approvals required for deployment"
      finalizeDeployment(config)
    }
  }
}
