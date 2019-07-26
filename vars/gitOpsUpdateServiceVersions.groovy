#!/usr/bin/env groovy
import net.sf.json.JSONObject
import com.github.banzaicicd.cfg.BanzaiCfg
import java.time.ZoneOffset
import java.time.OffsetDateTime

def call(BanzaiCfg cfg) {
	if (!cfg.gitOps) {
		logger "Current build is not a GitOps repo. Skipping gitOpsUpdateServiceVersions"
		return
	}
	if (!params.gitOpsTriggeringBranch) {
		logger "No params.gitOpsTriggeringBranch found. Will not attempt to update service versions."
		return
	}
	if (cfg.gitOps.skipVersionUpdating && BRANCH_NAME ==~ cfg.gitOps.skipVersionUpdating) {
		logger "skipVersionUpdating detected for branch '${params.gitOpsTriggeringBranch}'. Will not update versions"
		return
	}
	def SERVICE_DIR_NAME = "${WORKSPACE}/services"
	def ENV_DIR_NAME = "${WORKSPACE}/envs"

	// determine if this build qualifies for an autoDeploy
	// if so, prepare the necessary gitOps vars and mark deploy = true
	if (cfg.gitOps.autoDeploy) {
		def key = cfg.gitOps.autoDeploy.keySet().find { params.gitOpsTriggeringBranch ==~ it }
		if (key) {
			autoDepoyEnv = cfg.gitOps.autoDeploy[key]
			logger "gitOps.autoDeploy detected. Preparing cfg.gitOps properties"
			cfg.internal.gitOps.DEPLOY = true
			cfg.internal.gitOps.TARGET_ENV = autoDepoyEnv
			cfg.internal.gitOps.TARGET_STACK = params.gitOpsStackId
			cfg.internal.gitOps.SERVICE_VERSIONS_TO_UPDATE = [:]
			cfg.internal.gitOps.DEPLOYMENT_ID = OffsetDateTime.now(ZoneOffset.UTC) as String
		}
	}

	// parse the incoming service versions
	/**
		gitOpsVersions take the format
		{
			serviceId : {
				version: 1.0.0,
				meta: {} // <- optional
			}
		}
	*/
	def serviceVersions = readJSON(text: params.gitOpsVersions)
	// ensure services dir exists
	dir(SERVICE_DIR_NAME) {
		if (!fileExists("/")) {
			logger "No ${SERVICE_DIR_NAME} dir exists. Creating..."
			sh "mkdir ${SERVICE_DIR_NAME}"
		}
	}
	
	/*
		For each service version
		1. look up the service in services/
		2. if the version already exists, replace it in-place
		2a. else, preprend to the list of versions
		3. update the latest property
	*/
	def serviceIdsAndVersions = [] // formatted strings for logging later
	serviceVersions.each { id, data ->
		serviceIdsAndVersions.push("${id}:${data.version}")
		def serviceFileName = "${SERVICE_DIR_NAME}/${id}.yaml"
		if (!fileExists(serviceFileName)) {
			// if the <service>.yaml doesn't exist, create it.
			def yamlTemplate = [latest: '', versions: []]
			writeYaml file: serviceFileName, data: yamlTemplate
		}
		// serviceYaml.versions can contain String's or Objects
		def serviceYaml = readYaml file: serviceFileName
		serviceYaml.latest = data.version
		def versionList = serviceYaml.versions.collect {
			if (it instanceof String) {
				return it
			} else {
				return it.keySet()[0] 
			}
		}

		if (!versionList.contains(data.version)) {
			def newVersion
			if (data.meta) {
				newVersion = [:]
				newVersion[data.version] = data.meta
			} else {
				newVersion = data.version
			}

			serviceYaml.versions.add(0, newVersion)
		} else {
			// we need to overwrite the existing as it may have metadata that is changed
			int i = versionList.findIndexOf { it == data.version }
			def newVersion
			if (data.meta) {
				newVersion = [:]
				newVersion[data.version] = data.meta
			} else {
				newVersion = data.version
			}

			serviceYaml.versions[i] = newVersion
		}
		// writeYaml will fail if the file already exists
		sh "rm -rf ${serviceFileName}"
		logger "Updating Service '${id}' to '${data.version}'"
		writeYaml file: serviceFileName, data: serviceYaml

		// if this is an autoDeploy
		// update the services versions that we will eventually update in the env/stack if approval passes
		if (cfg.internal.gitOps.SERVICE_VERSIONS_TO_UPDATE) {
			cfg.internal.gitOps.SERVICE_VERSIONS_TO_UPDATE[id] = data.version
		}
	}

	// commit service updates
	dir (WORKSPACE) {
		if (cfg.gitOps.gitUser && cfg.gitOps.gitEmail) {
	      logger "git config user.name '${cfg.gitOps.gitUser}' && git config user.email '${cfg.gitOps.gitEmail}'"
	      sh "git config user.name '${cfg.gitOps.gitUser}' && git config user.email '${cfg.gitOps.gitEmail}'"
      	}
		
		def gitStatus = sh(returnStdout: true, script: 'git status')
		if (!gitStatus.contains('nothing to commit')) {
			sh "git add . && git commit -m 'Updating the following Services ${serviceIdsAndVersions}'"
			sh "git pull && git push origin master"
		} else {
			logger "No new Services versions commited to the GitOps repository."
		}
	}
}
