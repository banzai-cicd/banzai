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
	if (config.gitOps.autoDeploy) {
		def key = config.gitOps.autoDeploy.keySet().find { params.gitOpsTriggeringBranch ==~ it }
		if (key) {
			autoDepoyEnv = config.gitOps.autoDeploy[key]
			logger "gitOps.autoDeploy detected. Preparing config.gitOps properties"
			config.deploy = true // <-- VERY IMPORTANT THAT THIS IS SET
			config.gitOps.TARGET_ENV = autoDepoyEnv
			config.gitOps.TARGET_STACK = params.gitOpsStackId
			config.gitOps.SERVICE_VERSIONS_TO_UPDATE = [:]
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
	// We always update the /services versions regardless of a deployment
	serviceVersions.each { id, data ->
		serviceIdsAndVersions.push("${id}:${data.version}")
		def serviceFileName = "${SERVICE_DIR_NAME}/${id}.yaml"
		if (!fileExists(serviceFileName)) {
			def yamlTemplate = [latest: '', versions: []]
			writeYaml file: serviceFileName, data: yamlTemplate
		}

		def serviceYaml = readYaml file: serviceFileName
		serviceYaml.latest = data.version
		def versionList = serviceYaml.versions.collect {
			if (it instanceof String) {
				return it
			} else {
				return it.keySet()[0] 
			}
		} // each entry should have an object with a single key (the version) OR a String (the version)
		if (!versionList.contains(data.version)) {
			if (data.meta) {
				def versionObj = [:]
				versionObj[data.version] = data.meta
				serviceYaml.versions.add(0, versionObj)
			} else {
				serviceYaml.versions.add(0, data.version)
			}
			
		}
		// writeYaml will fail if the file already exists
		sh "rm -rf ${serviceFileName}"
		logger "Updating Service '${id}' to '${data.version}'"
		writeYaml file: serviceFileName, data: serviceYaml

		// if this is an autoDeploy
		// update the services versions that we will eventually update in the env/stack if approval passes
		if (config.gitOps.SERVICE_VERSIONS_TO_UPDATE) {
			config.gitOps.SERVICE_VERSIONS_TO_UPDATE[id] = data.version
		}
	}

	// commit service updates
	dir (WORKSPACE) {
		def gitStatus = sh(returnStdout: true, script: 'git status')
		if (!gitStatus.contains('nothing to commit')) {
			sh "git add . && git commit -m 'Updating the following Services ${serviceIdsAndVersions}'"
			sh "git pull && git push origin master"
		} else {
			logger "No new Services versions commited to the GitOps repository."
		}
	}
}
