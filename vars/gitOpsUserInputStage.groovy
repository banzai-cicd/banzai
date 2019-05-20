#!/usr/bin/env groovy

def versionSelectionStage(config, targetEnvironment, targetStack) {
  // for each service listed in the <stackId>.yaml ask for a version to use.
  def stackFileName = "${WORKSPACE}/${targetEnvironment}/${targetStack}.yaml"
	def	stackYaml = readYaml file: stackFileName
  def serviceIds = stackYaml.keySet()
  def params = serviceIds.collect {
    choice(name: "${it}Version", choices: "1.0.0\n1.1.1", description: 'What version of the Service should be deployed?')
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
  def FILE_WRAPPER_CLASS = 'org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper'
  def stageName = 'GitOps: User Input Stages'
  if (!config.gitOps || params.gitOpsTriggeringBranch != 'empty') {
      logger "Does not appear to be a user-initiated GitOps build. Skipping '${stageName}'"
      return
  }

  def targetEnvironment
  stage ('Target Environment?') {
    // get all of the envs listed in the repo
    def envs
    dir("${WORKSPACE}/envs") {
      envs = findFiles(glob: "*/")
    }
    if (!envs || envs.size() == 0) {
      logger "No environments found. Ensure that /envs is not empty"
      return
    }
    def envChoices = envs.collect { it.getName() }.join("\n")
    timeout(time: 10, unit: 'MINUTES') {
      script {
        targetEnvironment = input(
          id: 'targetEnvInput', 
          message: 'What Environment do you want to deploy to?',
          ok: 'Next Step',
          parameters: [choice(name: 'targetEnv', choices: envChoices, description: 'What is the target Environment?')]
        )
      }

      logger "Target Environment selected! ${targetEnvironment}"
    }
  }

  def targetStack
  stage ('Stack?') {
    def stackFiles
    dir("${WORKSPACE}/envs/${targetEnvironment}") {
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
  stage ('Deployment Style?') {
    def deploymentStyle
    timeout(time: 10, unit: 'MINUTES') {
      script {
        deploymentStyle = input(
          id: 'deploymentStyleInput', 
          message: 'What style of deployment?',
          ok: 'Next Step',
          parameters: [choice(name: 'deploymentType', choices: 'Version Selection\nEnvironment Promotion', description: 'What is the deployment style?')]
        )
      }

      logger "Choice selected! ${deploymentType}"
    }
  }

  switch (deploymentStyle) {
    case 'Version Selection':
      versionSelectionStage(config, targetEnvironment, targetStack)
      break
    case 'Environment Promotion':
      break
    default:
      logger "Unable to match deployment style selection"
      break
  }
}
