#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {
    stage ('Mark For Promotion') {
		
        if (config.markForPromotion) {
	    Pattern pattern = Pattern.compile(config.promoteBranches)

	    if (!(BRANCH_NAME ==~ pattern)) {
		println "${BRANCH_NAME} does not match the promoteBranches pattern. Skipping markForPromotion"
		return
	    }
	}
	  
	//def versionFileInfo = new File("${WORKSPACE}/versionInfo")
	def VER_FILE = "${WORKSPACE}/versionInfo"
	def VER_FILE_STATUS = sh (
	    script: 'if [ -e ${VER_FILE} ]; then echo "EXISTS" ; else echo "NOT-EXISTS" ; fi',
	    returnStdout: true
	).trim()
	echo "VER_FILE_STATUS: ${VER_FILE_STATUS}" 
	
	if (VER_FILE_STATUS == 'EXISTS') {
	    println "${WORKSPACE}/versionInfo detected"
	} else {
	    throw new IllegalArgumentException("no ${WORKSPACE}/versionInfo exists!")
	}
	// value = cat ${VER_FILE}; echo "$value";
	def VER_FILE_CONTENT = sh (
	    script: 'echo $(cat ${VER_FILE})',
	    returnStdout: true
	).trim()
	echo "VER_FILE_CONTENT: ${VER_FILE_CONTENT}" 
	
	def versionInfo = VER_FILE_CONTENT
	def BUILD_VERSION_QA = ''
	  
	if(versionInfo.toLowerCase().contains('|')) {
	    BUILD_VERSION_QA = versionInfo.tokenize('|')[1]
	} else {
	    BUILD_VERSION_QA = versionInfo
	}	  
	def deploymntRepoName =  config.deploymentRepo.tokenize('/').last().split("\\.")[0]	  
	echo "MARK-PROMOTION SCRIPT - Updating deploymntRepo for ${config.stackServiceName} module with version ${BUILD_VERSION_QA}"
	  
	sh "rm -rf ${deploymntRepoName}"
	sh "git clone ${config.deploymentRepo}"
	sh "yaml w -i ${deploymntRepoName}/envs/qa/version.yml version.${config.stackServiceName} ${BUILD_VERSION_QA}"
	sh "git -C ${deploymntRepoName} commit -a -m 'Updated QA version for ${config.stackServiceName} module' || true"
	sh "git -C ${deploymntRepoName} pull && git -C ${deploymntRepoName} push origin master"
	  
	echo "MARK-PROMOTION SCRIPT - Done updation for ${config.stackServiceName} module with version ${BUILD_VERSION_QA}"
    }
}
