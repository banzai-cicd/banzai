#!/usr/bin/env groovy

import groovy.json.JsonOutput
import net.sf.json.JSONObject

// Determine if gitOps job should run.
def call(config) {
	def gitOpsVersionsFileName = "${WORKSPACE}/gitOpsVersions"

	if (!fileExists(gitOpsVersionsFileName)) {
		error("No gitOpsVersions file exists!")
		return
	}
	def gitOpsVersions = readFile gitOpsVersionsFileName
	logger "gitOpsVersions"
	logger gitOpsVersions
	// convert gitOpsVersions to groovy map
	def versionsObj = [:]
	gitOpsVersions.trim().tokenize("\n").each {
		def serviceIdAndVersion = it.trim().tokenize(" ")
		// we should add or update the service to the /services dir.
		versionsObj[serviceIdAndVersion[0]] = serviceIdAndVersion[1]
	}
	
	// kick off gitops job and pass params
	def buildDef = [
		propagate: false,
		wait: false,
		job: config.gitOpsTrigger.jenkinsJob,
		parameters: [
			string(name: 'gitOpsVersions', value:  JsonOutput.toJson(versionsObj)),
			string(name: 'gitOpsTriggeringBranch', value: BRANCH_NAME),
			string(name: 'gitOpsStackId', value: config.gitOpsTrigger.stackId)
		]
	]

	logger "Triggering GitOps build downstream"
	build(buildDef)
}
