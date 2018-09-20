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
	  
	  def deploymntRepoName =  config.deploymentRepo.tokenize('/').last().split("\\.")[0]
	  
	  echo "MARK-PROMOTION SCRIPT - Updating deploymntRepo for ${config.stackServiceName} module with version $BUILD_VERSION"
	  sh """#!/bin/bash
		rm -rf ${deploymntRepoName}
		git ${config.deploymentRepo}
		yaml w -i "$WORKSPACE"/${deploymntRepoName}/envs/qa/version.yml version.${config.stackServiceName} $BUILD_VERSION
		git -C ${deploymntRepoName} commit -a -m 'Updated QA version for ${config.stackServiceName} module' || true
		git -C ${deploymntRepoName} pull && git -C ${deploymntRepoName} push origin master
	  """    		
	  echo "MARK-PROMOTION SCRIPT - Done updation for ${config.stackServiceName} module with version $BUILD_VERSION"
	}
}