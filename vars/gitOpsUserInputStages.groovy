#!/usr/bin/env groovy

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
    choice(name: "${it}", choices: choices, description: "current: ${stackYaml[it]}")
  }
  def selectedVersions
  stage ('Versions') {
    timeout(time: 10, unit: 'MINUTES') {
      script {
        selectedVersions = input(
          id: 'versionsInput', 
          message: "What Service version(s) should be assigned to the '${targetStack}' Stack in the '${targetEnvironment}' Environment?",
          ok: 'Next Step',
          parameters: params
        )
      }
    }
  }

  if (selectedVersions instanceof String) {
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
  /////
  // These Stages should only run for user-initiated builds of a GitOps repo
  /////
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
          parameters: [choice(name: 'Target Environment', choices: envChoices)]
        ).trim()
      }

      logger "Target Environment selected! ${targetEnvironment}"
      config.gitOps.TARGET_ENV = targetEnvironment
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
          message: "What Stack do you want to deploy to the '${targetEnvironment}' Environment?",
          ok: 'Next Step',
          parameters: [choice(name: 'Target Stack', choices: stackIdChoices)]
        )
      }

      logger "Target Stack selected! ${targetStack}"
      config.gitOps.TARGET_STACK = targetStack
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
          parameters: [choice(name: 'Deployment Style', choices: 'Select Versions\nPromote Environment')]
        )
      }

      logger "Choice selected! ${deploymentStyle}"
    }
  }

  /////
  // determine versions
  /////
  def versions
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
  logger "Versions Determined: ${versions}"
  config.gitOps.STACK_VERSIONS_TO_UPDATE = versions

  // IMPORTANT! we now are ready to set config.deploy = true because all deployment info has been satisfied
  config.deploy = true
}
