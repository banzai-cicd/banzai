#!/usr/bin/env groovy

def selectVersionsStage(config, targetEnvironment, targetStack) {
  String SERVICE_DIR_NAME = "${WORKSPACE}/services"
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  // for each service listed in the <stackId>.yaml ask for a version to use.
  String stackFileName = "${ENV_DIR_NAME}/${targetEnvironment}/${targetStack}.yaml"
	def	stackYaml = readYaml file: stackFileName
  String[] serviceIds = stackYaml.keySet()
  def params = serviceIds.collect {
    def serviceYaml = readYaml file: "${SERVICE_DIR_NAME}/${it}.yaml"
    def choices = serviceYaml.versions.join("\n")
    choice(name: "${it}Version", choices: choices, description: 'What version of the Service should be deployed?')
  }
  def selectedVersions
  stage ('Versions?') {
    timeout(time: 10, unit: 'MINUTES') {
      script {
        selectedVersions = input(
          id: 'versionsInput', 
          message: 'What version of each Service should be deployed?',
          ok: 'Next Step',
          parameters: params
        )
      }

      logger "Versions selected! ${selectedVersions}"
    }
  }
}


def call(config) {
  String SERVICE_DIR_NAME = "${WORKSPACE}/services"
  String ENV_DIR_NAME = "${WORKSPACE}/envs"
  def stageName = 'GitOps: User Input Stages'
  if (!config.gitOps || params.gitOpsTriggeringBranch != 'empty') {
      logger "Does not appear to be a user-initiated GitOps build. Skipping '${stageName}'"
      return
  }

  def targetEnvironment
  stage ('Target Environment?') {
    // get all of the envs listed in the repo

    String[] envChoices = []
    dir(ENV_DIR_NAME) {
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
    timeout(time: 10, unit: 'MINUTES') {
      script {
        targetEnvironment = input(
          id: 'targetEnvInput', 
          message: 'What Environment do you want to deploy to?',
          ok: 'Next Step',
          parameters: [choice(name: 'targetEnv', choices: envChoices, description: 'What is the target Environment?')]
        ).trim()
      }

      logger "Target Environment selected! ${targetEnvironment}"
    }
  }

  String targetStack
  stage ('Stack?') {
    def stackFiles
    dir("${ENV_DIR_NAME}/${targetEnvironment}") {
      stackFiles = findFiles(glob: "*.yaml")
    }
    if (!stackFiles || stackFiles.size() == 0) {
      logger "No stacks found. Ensure that /envs/${targetEnvironment} is not empty"
      return
    }
    def stackIdChoices = stackFiles.collect { it.getName().replace('.yaml', '') }.join("\n")
    timeout(time: 10, unit: 'MINUTES') {
      script {
        targetStack = input(
          id: 'targetStackInput', 
          message: 'What Stack do you want to deploy?',
          ok: 'Next Step',
          parameters: [choice(name: 'targetStack', choices: stackIdChoices, description: 'What is the target stack?')]
        )
      }

      logger "Target Stack selected! ${targetStack}"
    }
  }
  // prompt the user to determine which style of deployment they would like to achieve.
  // we will support 2 styles first. 'version-selection' and 'environment promotion'
  String deploymentStyle
  stage ('Deployment Style?') {
    timeout(time: 10, unit: 'MINUTES') {
      script {
        deploymentStyle = input(
          id: 'deploymentStyleInput', 
          message: 'What style of deployment?',
          ok: 'Next Step',
          parameters: [choice(name: 'deploymentStyle', choices: 'Select Versions\nPromote Environment', description: 'What is the deployment style?')]
        )
      }

      logger "Choice selected! ${deploymentStyle}"
    }
  }

  switch (deploymentStyle) {
    case 'Select Versions':
      selectVersionsStage(config, targetEnvironment, targetStack)
      break
    case 'Promote Environment':
      break
    default:
      logger "Unable to match deployment style selection"
      break
  }
}
