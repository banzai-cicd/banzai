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

Map<String, String> selectVersionsStage(config, targetEnvironment, targetStack) {
  String SERVICE_DIR_NAME = "${WORKSPACE}/services"
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  // for each service listed in the <stackId>.yaml ask for a version to use.
  String stackFileName = "${ENV_DIR_NAME}/${targetEnvironment}/${targetStack}.yaml"
	def	stackYaml = readYaml file: stackFileName
  String[] serviceIds = stackYaml.keySet()
  def params = serviceIds.collect {
    def serviceYaml = readYaml file: "${SERVICE_DIR_NAME}/${it}.yaml"
    def choices = serviceYaml.versions.join("\n")
    choice(name: "${it} (current: ${stackYaml[it]})", choices: choices, description: 'What version of the Service should be deployed?')
  }
  def selectedVersions
  stage ('Versions') {
    timeout(time: 10, unit: 'MINUTES') {
      script {
        selectedVersions = input(
          id: 'versionsInput', 
          message: 'What version of each Service should be deployed?',
          ok: 'Next Step',
          parameters: params
        )
      }

      logger "Versions selected! ${selectedVersions.getClass()}"
    }
  }

  if (selectedVersions.getClass != 'java.util.HashMap') {
    // only 1 service in a stack will result in selectedVersions being a string instead of a Map
    selectedVersions = ["${serviceIds[0]}": selectedVersions]
  }
  
  return selectedVersions
}

def isUserInitiated() {
  return currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause) != null
}

def call(config) {
  String SERVICE_DIR_NAME = "${WORKSPACE}/services"
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  def stageName = 'GitOps: User Input Stages'
  if (!config.gitOps || !isUserInitiated()) {
      logger "Does not appear to be a user-initiated GitOps build. Skipping '${stageName}'"
      return
  }

  /////
  // determine target env/stack and deployment style
  /////
  def targetEnvironment
  stage ('Target Environment') {
    // get all of the envs listed in the repo

    String envChoices
    dir (ENV_DIR_NAME) {
      envChoices = sh(
          script: "ls -d -- */ | sed 's/\\///g'",
          returnStdout: true
      ).trim()
    }

    logger "envChoices"
    logger envChoices
    if (!envChoices || envChoices.length() == 0) {
      logger "No environments found. Ensure that /envs is not empty"
      return
    }
    timeout (time: 10, unit: 'MINUTES') {
      script {
        targetEnvironment = input(
          id: 'targetEnvInput', 
          message: 'What Environment do you want to deploy to?',
          ok: 'Next Step',
          parameters: [choice(name: 'Target Environment', choices: envChoices, description: 'What is the target Environment?')]
        ).trim()
      }

      logger "Target Environment selected! ${targetEnvironment}"
    }
  }

  String targetStack
  stage ('Stack') {
    def stackFiles
    dir ("${ENV_DIR_NAME}/${targetEnvironment}") {
      stackFiles = findFiles(glob: "*.yaml")
    }
    if (!stackFiles || stackFiles.size() == 0) {
      def errMsg = "No stacks found. Ensure that /envs/${targetEnvironment} is not empty"
      logger errMsg
      currentBuild.result = 'ABORTED'
      error(errMsg)
      return
    }
    def stackIdChoices = stackFiles.collect { it.getName().replace('.yaml', '') }.join("\n")
    timeout (time: 10, unit: 'MINUTES') {
      script {
        targetStack = input(
          id: 'targetStackInput', 
          message: 'What Stack do you want to deploy?',
          ok: 'Next Step',
          parameters: [choice(name: 'Target Stack', choices: stackIdChoices, description: 'What is the target stack?')]
        )
      }

      logger "Target Stack selected! ${targetStack}"
    }
  }
  // prompt the user to determine which style of deployment they would like to achieve.
  // we will support 2 styles first. 'version-selection' and 'environment promotion'
  String deploymentStyle
  stage ('Deployment Style') {
    timeout (time: 10, unit: 'MINUTES') {
      script {
        deploymentStyle = input(
          id: 'deploymentStyleInput', 
          message: 'What style of deployment?',
          ok: 'Next Step',
          parameters: [choice(name: 'Deployment Style', choices: 'Select Versions\nPromote Environment', description: 'What is the deployment style?')]
        )
      }

      logger "Choice selected! ${deploymentStyle}"
    }
  }

  /////
  // determine versions
  /////
  Map versions
  switch (deploymentStyle) {
    case 'Select Versions':
      versions = selectVersionsStage(config, targetEnvironment, targetStack)
      break
    case 'Promote Environment':
      break
    default:
      logger "Unable to match deployment style selection"
      break
  }

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

  if (approverEmails && approverSSOs) {
    // notify approvers via email that there is a deployment
    // requested and provide an input step
    stage ("Approve Deployment to '${targetEnvironment}'") {
      timeout(time: 3, unit: 'DAYS') {
        def msg = "Deploy to '${targetEnvironment}'"
        script {
          try {
            def approvalResult = input message: msg,
              ok: 'Approve',
              submitter: approverSSOs,
              submitterParameter: 'submitter'
            // TODO: send email to approvers and watchers
            logger "Deployment to '${targetEnvironment}' approved by ${approvalResult.approver}"
          } catch (err) {
            logger "Deployment to '${targetEnvironment}' denied by ${err.message}"
            currentBuild.result = 'ABORTED'
            // TODO: send email to approvers and watchers
            error("Deployment to '${targetEnvironment}' denied by ${err.message}")
          }
        }
      }
    }
  }

}
