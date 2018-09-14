#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import static org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK

def call(config) {
	
	echo "Environment Selection"
	stage ('Environment Selection'){
		
		env.ENV_OPTION = ''
		/*timeout(time: 3, unit: 'DAYS') {
			script {
				env.ENV_OPTION = input message: "Select the Environment for Deployment",
						ok: 'Submit',
						parameters: [choice(name: 'Where do you want to deploy the application ?', choices: "QA&PROD\nQA\nPROD\nSkip", description: 'What would you like to do?')]
			}
		}*/
		env.ENV_OPTION = 'QA'
	}
	if (env.ENV_OPTION == 'Skip') {
		echo "You want to skip deployment!"
		return
	}
	
	if (env.ENV_OPTION.contains('QA')) {
	// Request QA deploy
	echo "Requesting QA deployment"
	stage ('Promote to QA ?'){
		
		/*if (config.promoteBranches) {
			Pattern pattern = Pattern.compile(config.promoteBranches)
	  
			if (!(BRANCH_NAME ==~ pattern)) {
			  println "${BRANCH_NAME} does not match the promoteBranches pattern. Skipping Promote"
			  return
			}
		}*/
		  
		env.DEPLOY_OPTION = ''
		/*timeout(time: 3, unit: 'DAYS') {
			script {
				env.DEPLOY_OPTION = input message: "Promote to QA ?",
						ok: 'Submit',
						parameters: [choice(name: 'Deployment Request', choices: "Deploy\nSkip", description: 'What would you like to do?')]
			}
		}*/
		env.DEPLOY_OPTION = 'Deploy'
	}
	if(env.DEPLOY_OPTION == 'Skip') {
		echo "You want to skip QA deployment!"
		return
	}
	else if(env.DEPLOY_OPTION == 'Deploy') {
		echo "You want to deploy in QA!"
		def environment = 'qa'
		promoteRepo = config.promoteRepo
		node(){
			sshagent (credentials: config.sshCreds) {
				stage ("QA Deployment") {
				  //runDeploy(config, 'QA') // Add QA param
										
					sh 'rm -rf config-reviewer-deployment'
					sh "git clone ${config.promoteRepo}"
					sh "pwd"
					
					//versionYmlData = readYaml file: "${WORKSPACE}/config-reviewer-deployment/envs/${environment}/version.yml"
					//assert mydata.versions == '3.14.0'
					//sh "yaml w -i config-reviewer-deployment/${environment}/version.yml version.${imageName} ${tag}"
					
					stackYmlData = readYaml file: "${WORKSPACE}/config-reviewer-deployment/envs/${environment}/config-reviewer-3.14.x.yml"
					versionYmlData = readYaml file: "${WORKSPACE}/config-reviewer-deployment/envs/${environment}/version.yml"
					
					versionYmlData.version.each{key, value ->
						existingImgName = stackYmlData.services[key].image
						echo ("Before Update image: "+stackYmlData.services[key].image)
						existingImgVersion = existingImgName.split(/:/)[-1]
						newImgVersion = value
						newImgName = stackYmlData.services[key].image.replaceAll(existingImgVersion, newImgVersion)
						echo ("Before Update image: "+newImgName)
						stackYmlData.services[key].image = newImgName
						sh "yaml w -i '${WORKSPACE}/config-reviewer-deployment/envs/${environment}/config-reviewer-3.14.x.yml' services.${key}.image ${newImgName}"
					}
					def paramList = []
					stackYmlData.services.each{ serviceName,value ->
						def uiParameter = [$class: 'TextParameterDefinition', defaultValue: stackYmlData.services[serviceName].image.split(/:/)[-1], description: serviceName, name: serviceName]
						paramList.add(uiParameter)
						print serviceName;
						echo ("image: "+stackYmlData.services[serviceName].image)
						echo ("version: "+versionYmlData.version[serviceName])
					}
					//def theName = a.split(/:/)[-1]
					//writeYaml file: "${WORKSPACE}/config-reviewer-deployment/envs/${environment}/config-reviewer-3.14.x.yml", data: stackYmlData
					
					sh "git -C config-reviewer-deployment commit -a -m 'Promoted QA Environment' || true"
					sh "git -C config-reviewer-deployment pull && git -C config-reviewer-deployment push origin master"

					def userInput = input(
						id: 'userInput', message: 'Verify module tags to be deployed', parameters: paramList)
					   echo ("Env: "+userInput['cr-api'])
					   echo ("Target: "+userInput['cr-service'])
					
					
				  echo "Deployed to QA!"
				}
			}
		}
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
						parameters: [choice(name: 'Deployment Request', choices: "Email Roger\nSkip", description: 'What would you like to do?')]
			}
		}
	}
	if(env.DEPLOY_OPTION == 'Skip') {
		echo "You want to skip PROD deployment!"
	}
	else if(env.DEPLOY_OPTION == 'Email Roger') {
		// If Request QA Deploy, dispatch approval request to QA team
		//submitter: '210026212' //Roger's SSO // ,Roger.Laurence@ge.com
		//"Roger.Laurence@ge.com"
		echo "You want to request PROD deployment!"
		stage ('Promote to PROD ?'){
			// Remove the app name hardcoding
			mail from: "JenkinsAdmin@ge.com",
				 to: "ramesh.ganapathi@ge.com",
				 cc: "ramesh.ganapathi@ge.com",
				 subject: "Config Reviewer Service awaiting PROD deployment approval",
				 body: "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' is waiting for PROD approval.\n\nPlease click the link below to proceed.\n${env.BUILD_URL}input/"
  
			env.DEPLOY_OPTION = ''
			timeout(time: 7, unit: 'DAYS') {
				script {
					env.DEPLOY_OPTION = input message: "Deploy Config Reviewer to PROD?",
							ok: 'Deploy to PROD!',
							parameters: [choice(name: 'Deployment Action', choices: "Deploy\nSkip", description: 'What would you like to do?')],
							submitter: '502061514' //Roger's SSO 210026212
				}
			}
		}
  
		if(env.DEPLOY_OPTION == 'Skip') {
			script.echo "You want to reject PROD deployment!"
		}
		else if(env.DEPLOY_OPTION == 'Deploy') {
			echo "You want to deploy in PROD!"
  
			// If approved, deploy to PROD
			echo "You want to deploy in PROD!"
			node(){
				sshagent (credentials: config.sshCreds) {
					stage ("PROD Deployment") {
					  //runDeploy(config, 'PROD') // Add QA param
					  echo "Deployed to PROD!"
					}
				}
			}
		}
	 }
	} // if
}
