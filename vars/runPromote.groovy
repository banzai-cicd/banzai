#! /usr/bin/env groovy

@NonCPS
def updateStackYamlVersion(versionYmlData, stackYmlData) {
	versionYmlData.version.each{key, value ->
		if (stackYmlData.services.containsKey(key))	{
			def existingImgName = stackYmlData.services[key].image
			def existingImgVersion = existingImgName.split(/:/)[-1]
			if(!(existingImgVersion.toLowerCase().contains('.com'))) {  // Ignore if no tag present with image name
				def newImgVersion = value
				def newImgName = stackYmlData.services[key].image.replaceAll(existingImgVersion, newImgVersion)
				stackYmlData.services[key].image = newImgName
			}
		}
	}	
	echo "Stack Yml Data after updating from Version file: ${stackYmlData.toMapString()}"
}

@NonCPS
def prepareUIList(stackYmlData) {
	stackYmlData.services.each{ serviceName,value ->
		def imgVersion = stackYmlData.services[serviceName].image.split(/:/)[-1]
		if(imgVersion.toLowerCase().contains('.com')) {    // Set empty if no tag present with image name
			imgVersion = ''
		}
		def uiParameter = [$class: 'StringParameterDefinition', defaultValue: imgVersion, description: "Please verify tag for Docker service ${serviceName}", name: serviceName]
		paramList.add(uiParameter)
	}
}

@NonCPS
def updateUserVersionInYaml(stackYmlData, userVersionInfo) {
	def serviceImgList = []
	stackYmlData.get('services').each{ serviceName,value ->
		def existingImgVersion = stackYmlData.services[serviceName].image.split(/:/)[-1]
		if(existingImgVersion.toLowerCase().contains('.com')) {
			existingImgVersion = ''
		}		
		newImgVersion = userVersionInfo[serviceName]
		newImgName = stackYmlData.services[serviceName].image.replaceAll(existingImgVersion, newImgVersion)		
		serviceImgList.add("${serviceName}~${newImgName}")
	}
	return serviceImgList
}

def call(config, environment) {
	
	paramList = []
	deploymntRepoName = ''
	userVersionInfo = [:]
	node(){
		sshagent (credentials: config.sshCreds) {
			stage ("Preparing for Deployment") {
				
				deploymntRepoName =  config.promoteRepo.tokenize('/').last().split("\\.")[0]
				echo "deploymntRepoName: ${deploymntRepoName}"
				
				sh "rm -rf ${deploymntRepoName}"
				sh "git clone ${config.promoteRepo}"
				
				stackYmlData = readYaml file: "${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml"
				versionYmlData = readYaml file: "${WORKSPACE}/${deploymntRepoName}/envs/${environment}/version.yml"
								
				updateStackYamlVersion(versionYmlData, stackYmlData)				
				prepareUIList(stackYmlData)
			}
		}
	}
	stage ("Verify Deployment Info") {
		timeout(time: 3, unit: 'DAYS') {
			script {
				userVersionInfo = input(id: 'userInput', message: "Verify ${config.stackName} application module tags to be deployed to ${environment.toUpperCase()}", parameters: paramList)
			}
		}
	}
	node(){
		sshagent (credentials: config.sshCreds) {
			stage ("${environment.toUpperCase()} Deployment") {
			   def stackYmlData = readYaml file: "${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml"
			   def serviceImgList = updateUserVersionInYaml(stackYmlData, userVersionInfo)
							
			   for (int i = 0; i < serviceImgList.size(); i++) {
				   def serviceImg = serviceImgList[i].split(/~/)
				   sh "yaml w -i '${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml' services.${serviceImg[0]}.image ${serviceImg[1]}"
				   echo "Updating YAML Service: ${serviceImg[0]} with Image: ${serviceImg[1]}"
			   }
			   
			   sh "git -C ${deploymntRepoName} commit -a -m 'Promoted ${environment.toUpperCase()} Environment' || true"
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
			   
			   echo "scp '${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml' ${deployUser}@${deployServer}:${appStackYmlPath}"
			   echo "ssh -o StrictHostKeyChecking=no ${deployUser}@${deployServer} ${deployScript}"
			   
			   sh "scp '${WORKSPACE}/${deploymntRepoName}/envs/${environment}/${config.stackName}-dev.yml' ${deployUser}@${deployServer}:${appStackYmlPath}"
			   sh "ssh -o StrictHostKeyChecking=no ${deployUser}@${deployServer} '${deployScript}'"
				
			   echo "Deployed to ${environment.toUpperCase()}!"
			}
		}
	}
}	
	
