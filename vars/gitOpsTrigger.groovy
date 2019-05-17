#!/usr/bin/env groovy
import groovy.json.JsonOutput
import net.sf.json.JSONObject

// Determine if gitOps job should run.
def call(config) {
	//def versionFileInfo = new File("${WORKSPACE}/gitOpsVersions")
	def VER_FILE = "${WORKSPACE}/gitOpsVersions"
	def VER_FILE_STATUS = sh (
	    script: 'if [ -e ${VER_FILE} ]; then echo "EXISTS" ; else echo "NOT-EXISTS" ; fi',
	    returnStdout: true
	).trim()
	echo "VER_FILE_STATUS: ${VER_FILE_STATUS}" 
	
	if (VER_FILE_STATUS == 'EXISTS') {
	    logger "${WORKSPACE}/gitOpsVersions detected"
	} else {
	    throw new IllegalArgumentException("no ${WORKSPACE}/gitOpsVersions exists!")
	}

	def gitOpsVersions = readFile "${WORKSPACE}/gitOpsVersions"
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
			string(name: 'gitOpsTriggeringBranch', value: BRANCH_NAME)
		]
	]

	logger "Triggering GitOps build downstream"
	build(buildDef)
}
