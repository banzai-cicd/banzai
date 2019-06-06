#!/usr/bin/env groovy
import groovy.json.JsonOutput
import net.sf.json.JSONObject
import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiGitOpsTriggerCfg

// Determine if gitOps job should run.
def call(Map userData, BanzaiGitOpsTriggerCfg gitOpsCfg) {
	if (!userData || !userData.gitOps || !userData.gitOps.versions) {
		logger "No gitOps.versions detected in BanzaiUserData"
		return
	}
	// kick off gitops job and pass params
	def buildDef = [
		propagate: false,
		wait: false,
		job: gitOpsCfg.jenkinsJob,
		parameters: [
			string(name: 'gitOpsVersions', value:  JsonOutput.toJson(userData.gitOps.versions)),
			string(name: 'gitOpsTriggeringBranch', value: BRANCH_NAME),
			string(name: 'gitOpsStackId', value: gitOpsCfg.stackId)
		]
	]

	logger "Triggering GitOps build downstream"
	build(buildDef)
}
