#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern

def call(config) {

    // now build, based on the configuration provided
    stage ('Publish') {
      if (config.publishBranches) {
        def publishBranchesPattern = config.publishBranches
        Pattern pattern = Pattern.compile(publishBranchesPattern)

        if (!(BRANCH_NAME ==~ pattern)) {
          echo "${BRANCH_NAME} does not match the publishBranches pattern. Skipping Publish"
          return
        }
      }

      def PUBLISH_SCRIPT_FILE = config.publishScriptFile

      //Variable Error Handeling
      if(!PUBLISH_SCRIPT_FILE){
        throw new IllegalArgumentException("Jenkinsfile Variable must be configured: publishScriptFile")
      }

      //Modify Variable to ensure path starts with "./"
      if(PUBLISH_SCRIPT_FILE.charAt(0) == "/"){
        PUBLISH_SCRIPT_FILE = "." + PUBLISH_SCRIPT_FILE
      }
      if(PUBLISH_SCRIPT_FILE.charAt(0) != "."){
        PUBLISH_SCRIPT_FILE = "./" + PUBLISH_SCRIPT_FILE
      }

      println "Running publishScript..."
      println "var: ${WORKSPACE}/${PUBLISH_SCRIPT_FILE}"

      sh """#!/bin/bash
        if [ -f "${WORKSPACE}/${PUBLISH_SCRIPT_FILE}" ] ; then
          /bin/sh ${WORKSPACE}/${PUBLISH_SCRIPT_FILE} ${DOCKER_REPO_URL} ${config.appName}
        else
          echo "'${WORKSPACE}/${PUBLISH_SCRIPT_FILE}' does not exist!"
          exit 0
        fi
      """
    }
}
