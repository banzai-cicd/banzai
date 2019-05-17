#!/usr/bin/env groovy
import net.sf.json.JSONObject

def call(config) {
	// parse service versions obj from params
	if (config.gitOps.skipVersionUpdating && BRANCH_NAME ==~ config.gitOps.skipVersionUpdating) {
		logger "skipVersionUpdating detected for branch '${params.gitOpsTriggeringBranch}'. Will not update versions"
		return
	}

	def serviceVersions = readJSON(text: params.gitOpsVersions)
	/*
		For each service version
		1. look up the service in services/
		2. append to the list of versions
		3. update the latest property
	*/
	// ensure services dir exists
	def serviceDirName = "${WORKSPACE}/services"
	dir(serviceDirName) {
		if (!fileExists("/")) {
			logger "No ${serviceDirName} dir exists. Creating..."
			sh "mkdir ${serviceDirName}"
		}
	}
	
	def serviceIdsAndVersions = [] // for logging later
	serviceVersions.each { id, version ->
		serviceIdsAndVersions.push("${id}:${version}")
		def serviceFileName = "${serviceDirName}/${id}.yaml"
		if (!fileExists(serviceFileName)) {
			def yamlTemplate = [latest: '', versions: []]
			writeYaml file: serviceFileName, data: yamlTemplate
		}

		def yaml = readYaml file: serviceFileName
		yaml.latest = version
		yaml.versions.add(0, version)
		sh "rm -rf ${serviceFileName}"
		logger "Updating Service '${id}' to '${version}'"
		writeYaml file: serviceFileName, data: yaml
	}

	// commit service updates
	sh "git commit -a -m 'Updating the following Services ${serviceIdsAndVersions}' || true"
	sh "git pull && git ${deploymntRepoName} push origin master"
}
