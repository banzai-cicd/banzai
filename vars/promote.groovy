#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern
//import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import hudson.model.User

@NonCPS
def getRoleBasedUsersList(role) {
    echo "Retrieving users for ${role}..."
    def users = [:]
    def authStrategy = Jenkins.instance.getAuthorizationStrategy()
    if(authStrategy instanceof com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy){
      def sids = authStrategy.roleMaps.globalRoles.getSidsForRole(role)
      sids.each { sid ->        
	User usr = Jenkins.instance.getUser(sid)
	def usrmail = usr.getProperty(hudson.tasks.Mailer.UserProperty.class)
	if (usrmail.getAddress()) {
	    users[sid] = usrmail.getAddress()
	}
	//Jenkins.instance.getUser(sid).fullName
	echo "${sid}: ${usrmail.getAddress()}"
      }
      return users
    } else {
	throw new Exception("Role Strategy Plugin not in use.  Please enable to retrieve users for a role")
    }
}

def call(config) {

    def watchListEmail = ''
    def approverEmail = ''
    def approverSSO = ''
	
	echo "Environment Selection"
	stage ('Environment Selection'){
		
		if (!config.deploymentRepo || !config.stackName) {
			logger "'promoteRepo' and 'stackName' are required in your Jenkinsfile when 'promote' = true"
			return
		}
		
		if (config.promoteBranches && env.BRANCH_NAME) {
			Pattern pattern = Pattern.compile(config.promoteBranches)
			logger "Inside banch check"
			if (!(env.BRANCH_NAME ==~ pattern)) {
			   logger "${env.BRANCH_NAME} does not match the promoteBranches pattern. Skipping Promote"
			   return
			}
		}
		
		env.ENV_OPTION = ''
		timeout(time: 3, unit: 'DAYS') {
			script {
				env.ENV_OPTION = input message: "Select the Environment for Deployment",
						ok: 'Submit',
						parameters: [choice(name: 'Where do you want to deploy the application ?', choices: "QA\nPROD\nQA & PROD\nSkip", description: 'What would you like to do?')]
			}
		}
		if (config.approverEmail && config.approverSSO) {
		    watchListEmail = config.approverEmail
		    approverEmail = config.approverEmail
		    approverSSO = config.approverSSO
		}
		if (config.approverRole || config.watchlistRole) {
		    def approverMap = getRoleBasedUsersList(config.approverRole)
		    def watchListMap = getRoleBasedUsersList(config.watchlistRole)
		    echo "approverMap: ${approverMap.toMapString()}"
		    echo "watchListMap: ${watchListMap.toMapString()}"
		    
		    watchListEmail = watchListMap.values().join(",")
		    approverEmail = approverMap.values().join(",")
		    approverSSO = approverMap.keySet().join(",")
		}
	}
	if (env.ENV_OPTION == 'Skip') {
		echo "You want to skip deployment!"
		return
	}
	
	if (env.ENV_OPTION.contains('QA')) {
		// Request QA deploy
		echo "Requesting QA deployment"
		stage ('Promote to QA ?'){
			  
			env.DEPLOY_OPTION = ''
			timeout(time: 3, unit: 'DAYS') {
				script {
					env.DEPLOY_OPTION = input message: "Promote to QA ?",
							ok: 'Submit',
							parameters: [choice(name: 'Deployment Request', choices: "Deploy\nSkip", description: 'What would you like to do?')]
				}
			}
		}
		if(env.DEPLOY_OPTION == 'Skip') {
			echo "You want to skip QA deployment!"
			return
		}
		else if(env.DEPLOY_OPTION == 'Deploy') {
			echo "You want to deploy in QA!"		
			runPromote(config, 'qa')
			echo "Deployed to QA!"
			
			mail from: "JenkinsAdmin@ge.com",
				 to: watchListEmail,
				 subject: "QA deployment completed for ${config.stackName} application stack",
				 body: "QA deployment Info:\n\nApplication Stack: ${config.stackName}\nJob: ${env.JOB_NAME} [${env.BUILD_NUMBER}] \n\nBuild URL: ${env.BUILD_URL}"
		}
	}
	
	if (env.ENV_OPTION.contains('PROD')) {
		// Request PROD deploy
		echo "Requesting PROD deployment"
		stage ('Request PROD Deployment ?'){
			env.DEPLOY_OPTION = ''
			timeout(time: 5, unit: 'DAYS') {
				script {
					env.DEPLOY_OPTION = input message: "Request deployment to PROD ?",
							ok: 'Submit',
							parameters: [choice(name: 'Deployment Request', choices: "Email Approver\nSkip", description: 'What would you like to do?')]
				}
			}
		}
		if(env.DEPLOY_OPTION == 'Skip') {
			echo "You want to skip PROD deployment!"
		}
		else if(env.DEPLOY_OPTION == 'Email Approver') {			
			//submitter: '210026212' //Roger's SSO // ,Roger.Laurence@ge.com
			echo "You want to request PROD deployment!"
			stage ('Promote to PROD ?'){
				// Remove the app name hardcoding
				mail from: "JenkinsAdmin@ge.com",
					 to: approverEmail,
					 cc: watchListEmail,
					 subject: "${config.stackName} application stack AWAITING PROD deployment approval",
					 body: "Dear Approver(s), \n\nJob '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is waiting for PROD approval.\nPlease click the link below to proceed.\n${env.BUILD_URL}input/"
	  
				env.DEPLOY_OPTION = ''
				timeout(time: 7, unit: 'DAYS') {
					script {
						env.DEPLOY_OPTION = input message: "Deploy ${config.stackName} to PROD?",
								ok: 'Deploy to PROD!',
								parameters: [choice(name: 'Deployment Action', choices: "Deploy\nSkip", description: 'What would you like to do?')],
								submitter: approverSSO //Roger's SSO 210026212
					}
				}
			}
  
			if(env.DEPLOY_OPTION == 'Skip') {
			    script.echo "You want to reject PROD deployment!"
			    mail from: "JenkinsAdmin@ge.com",
					 to: watchListEmail,
					 cc: approverEmail,
					 subject: "${config.stackName} application stack REJECTED for PROD deployment",
					 body: "Team, \n\nJob '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is REJECTED for PROD deployment.\nBuild URL: ${env.BUILD_URL}"
			}
			else if(env.DEPLOY_OPTION == 'Deploy') {
				echo "You want to deploy in PROD!"  
				mail from: "JenkinsAdmin@ge.com",
					 to: watchListEmail,
					 cc: approverEmail,
					 subject: "${config.stackName} application stack APPROVED for PROD deployment",
					 body: "Team, \n\nJob '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is APPROVED for PROD deployment.\nBuild URL: ${env.BUILD_URL}"
	  				
				runPromote(config, 'prod')
				echo "Deployed to PROD!"
				
				mail from: "JenkinsAdmin@ge.com",
					 to: watchListEmail,
					 cc: approverEmail,
					 subject: "PROD deployment completed for ${config.stackName} application stack",
					 body: "PROD deployment Info:\n\nApplication Stack: ${config.stackName}\nJob: ${env.JOB_NAME} [${env.BUILD_NUMBER}] \n\nBuild URL: ${env.BUILD_URL}"
			}
		}
	}
}
