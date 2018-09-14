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
		environment = 'qa'
		paramList = []
		deploymntRepoName = ''
		
		node(){
			sshagent (credentials: config.sshCreds) {
				stage ("Preparing for Deployment") {
				  //runDeploy(config, 'QA') // Add QA param	
					
					deploymntRepoName =  config.promoteRepo.tokenize('/').last().split("\\.")[0]
					echo "deploymntRepoName: ${deploymntRepoName}"
					
					sh "rm -rf ${deploymntRepoName}"
					sh "git clone ${config.promoteRepo}"
					
					stackYmlData = readYaml file: "${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml"
					versionYmlData = readYaml file: "${WORKSPACE}/${deploymntRepoName}/envs/${environment}/version.yml"					
					
					versionYmlData.version.each{key, value -> 
						if (stackYmlData.services.containsKey(key))	{
							existingImgName = stackYmlData.services[key].image
							echo ("Before image Update: "+existingImgName)
							existingImgVersion = existingImgName.split(/:/)[-1]
							if(!(existingImgVersion.toLowerCase().contains('.com'))) {
								newImgVersion = value
								newImgName = stackYmlData.services[key].image.replaceAll(existingImgVersion, newImgVersion)
								echo ("After image Update: "+newImgName)
								stackYmlData.services[key].image = newImgName
							}
						}				    
					    			    
					}														
					stackYmlData.services.each{ serviceName,value -> 
						def imgVersion = stackYmlData.services[serviceName].image.split(/:/)[-1]
						echo ("existingImgVersion1: "+imgVersion)
						if(imgVersion.toLowerCase().contains('.com')) {
							imgVersion = ''
						}
						echo ("existingImgVersion2: "+imgVersion)
					    def uiParameter = [$class: 'StringParameterDefinition', defaultValue: imgVersion, description: "Please verify tag for Docker service ${serviceName}", name: serviceName]
					    paramList.add(uiParameter)
					    
					    echo ("Adding UI image: "+stackYmlData.services[serviceName].image)
					    echo ("Adding UI version: "+versionYmlData.version[serviceName])					    
					}
				}
			}
		}
		stage ("Verify Deployment Info") {
					env.VERSION_INFO = [:]
					timeout(time: 3, unit: 'DAYS') {
						script {
							env.VERSION_INFO = input(id: 'userInput', message: "Verify ${config.stackName} application module tags to be deployed to ${environment.toUpperCase()}", parameters: paramList)
						}
					}
					//def userInput = input(id: 'userInput', message: 'Verify module tags to be deployed', parameters: paramList)
					//println ("Env: "+env.VERSION_INFO['cr-api'])
					//println ("Target: "+env.VERSION_INFO['cr-service'])
		}		
			node(){
				sshagent (credentials: config.sshCreds) {
					stage ("QA Deployment") {						
						script {
							stackYmlData = readYaml file: "${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml"
						    stackYmlData.services.each{ serviceName,value ->
							   def existingImgVersion = stackYmlData.services[serviceName].image.split(/:/)[-1]
							   if(existingImgVersion.toLowerCase().contains('.com')) {
								   existingImgVersion = ''
							   }
							   newImgVersion = env.VERSION_INFO[serviceName]
							   newImgName = stackYmlData.services[key].image.replaceAll(existingImgVersion, newImgVersion)
							   sh "yaml w -i '${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml' services.${serviceName}.image ${newImgName}"
							   echo ("Updating YAML Service: ${serviceName} Version: "+stackYmlData.services[serviceName].image)					   
						   }
						}
				   
				   sh "git -C ${deploymntRepoName} commit -a -m 'Promoted QA Environment' || true"
				   sh "git -C ${deploymntRepoName} pull && git -C ${deploymntRepoName} push origin master"
				   
				   qaDeployServer="vdcald05143.ics.cloud.ge.com" //"vdcalq05504.ics.cloud.ge.com"
				   prodDeployServer="vdcald05143.ics.cloud.ge.com" //"vdcalq05504.ics.cloud.ge.com"
				   appStackYmlPath="~/docker-swarm/${config.stackName}"
				   appStackYml="${appStackYmlPath}/${config.stackName}-dev.yml"
				   deployCmd="docker stack deploy -c ${appStackYml} ${config.stackName} --with-registry-auth"
				   deployScript="docker login registry.gear.ge.com -u 502061514 -p password && ${deployCmd} && docker logout"
				   deployUser="de589146"
				   
				   deployServer = ''
				   if (environment == "qa") {
					   deployServer = qaDeployServer
				   } else if (environment == "prod") {
					   deployServer = prodDeployServer
				   }
				   
				   echo "scp ${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml ${deployUser}@${deployServer}:${appStackYmlPath}"
				   echo "ssh -o StrictHostKeyChecking=no ${deployUser}@${deployServer} ${deployScript}"
				   
				   sh "scp ${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml ${deployUser}@${deployServer}:${appStackYmlPath}"	
				   sh "ssh -o StrictHostKeyChecking=no ${deployUser}@${deployServer} ${deployScript}"
					
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
