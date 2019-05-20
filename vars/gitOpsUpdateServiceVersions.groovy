#!/usr/bin/env groovy
import net.sf.json.JSONObject

def call(config) {
	if (!config.gitOps) {
		logger "Current build is not a GitOps repo. Skipping gitOpsUpdateServiceVersions"
		return
	}
	if (!params.gitOpsTriggeringBranch) {
		logger "No params.gitOpsTriggeringBranch found. Will not attempt to update service versions."
		return
	}
	// parse service versions obj from params
	if (config.gitOps.skipVersionUpdating && BRANCH_NAME ==~ config.gitOps.skipVersionUpdating) {
		logger "skipVersionUpdating detected for branch '${params.gitOpsTriggeringBranch}'. Will not update versions"
		return
	}
	def SERVICE_DIR_NAME = "${WORKSPACE}/services"
	def ENV_DIR_NAME = "${WORKSPACE}/envs"

	// determine if we should autoDeploy this in addition to updating service versions
	def autoDepoyEnv
	if (config.gitOps.autoDeploy) {
		def key = config.gitOps.autoDeploy.keySet().find { params.gitOpsTriggeringBranch ==~ it }
		if (key) {
			autoDepoyEnv = config.gitOps.autoDeploy[key]
			logger "gitOps.autoDeploy detected. Will update ${autoDepoyEnv}/${params.gitOpsStackId}.yaml"
			config.deploy = true
		}
	}

	def serviceVersions = readJSON(text: params.gitOpsVersions)
	/*
		For each service version
		1. look up the service in services/
		2. append to the list of versions
		3. update the latest property
	*/
	// ensure services dir exists
	dir(SERVICE_DIR_NAME) {
		if (!fileExists("/")) {
			logger "No ${SERVICE_DIR_NAME} dir exists. Creating..."
			sh "mkdir ${SERVICE_DIR_NAME}"
		}
	}
	
	def serviceIdsAndVersions = [] // for logging later
	def stackYaml
	def stackFileName
	if (autoDepoyEnv) {
		stackFileName = "${ENV_DIR_NAME}/${autoDepoyEnv}/${params.gitOpsStackId}.yaml"
		stackYaml = readYaml file: stackFileName
	}
	serviceVersions.each { id, version ->
		serviceIdsAndVersions.push("${id}:${version}")
		def serviceFileName = "${SERVICE_DIR_NAME}/${id}.yaml"
		if (!fileExists(serviceFileName)) {
			def yamlTemplate = [latest: '', versions: []]
			writeYaml file: serviceFileName, data: yamlTemplate
		}

		def serviceYaml = readYaml file: serviceFileName
		serviceYaml.latest = version
		if (!serviceYaml.versions.contains(version)) {
			yaml.versions.add(0, version)
		}
		// writeYaml will fail if the file already exists
		sh "rm -rf ${serviceFileName}"
		logger "Updating Service '${id}' to '${version}'"
		writeYaml file: serviceFileName, data: serviceYaml

		// if stackFile exists then update it as well as this is an autoDeploy
		if (stackYaml) {
			stackYaml[id] = version
		}
	}
	if (stackYaml) {
		// save the updated stack yaml
		sh "rm -rf ${stackFileName}"
		writeYaml file: stackFileName, data: stackYaml
	}

	// commit service updates
	dir(WORKSPACE) {
		def gitStatus = sh(returnStdout: true, script: 'git status')
		if (!gitStatus.contains('nothing to commit')) {
			sh "git add . && git commit -m 'Updating the following Services ${serviceIdsAndVersions}'"
			sh "git pull && git push origin master"
		} else {
			logger "No new Services versions commited to the GitOps repository."
		}
	}
}
