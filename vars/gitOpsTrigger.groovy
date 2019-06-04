#!/usr/bin/env groovy
import groovy.json.JsonOutput
import net.sf.json.JSONObject
import com.ge.nola.BanzaiGitOpsTriggerCfg

// Determine if gitOps job should run.
def call(BanzaiGitOpsTriggerCfg gitOpsCfg) {
	def gitOpsVersionsFileName = "${WORKSPACE}/gitOpsVersions"
	def gitOpsVersions
	if (fileExists("${gitOpsVersionsFileName}.yaml")) {
		gitOpsVersions = readYaml file: "${gitOpsVersionsFileName}.yaml"
	} else if (fileExists("${gitOpsVersionsFileName}.json")) {
		gitOpsVersions = readJSON file: "${gitOpsVersionsFileName}.json"
	} else {
		error("No gitOpsVersions.{yaml/json} exists!")
		return
	}

	// kick off gitops job and pass params
	def buildDef = [
		propagate: false,
		wait: false,
		job: gitOpsCfg.jenkinsJob,
		parameters: [
			string(name: 'gitOpsVersions', value:  JsonOutput.toJson(gitOpsVersions)),
			string(name: 'gitOpsTriggeringBranch', value: BRANCH_NAME),
			string(name: 'gitOpsStackId', value: gitOpsCfg.stackId)
		]
	]

	logger "Triggering GitOps build downstream"
	build(buildDef)
}
