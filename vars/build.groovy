def call(config) {
  
    // now build, based on the configuration provided
    stage ('Build') {
      def BUILD_SCRIPT_FILE = config.buildScriptFile

      //Variable Error Handeling
      if(!BUILD_SCRIPT_FILE){
        throw new IllegalArgumentException("Jenkinsfile Variable must be configured: BUILD_SCRIPT_FILE")
      }

      //Modify Variable to ensure path starts with "./"
      if(BUILD_SCRIPT_FILE.charAt(0) == "/"){
        BUILD_SCRIPT_FILE = "." + BUILD_SCRIPT_FILE
      }
      if(BUILD_SCRIPT_FILE.charAt(0) != "."){
        BUILD_SCRIPT_FILE = "./" + BUILD_SCRIPT_FILE
      }

      println "Running build..."
      println "var: ${WORKSPACE}/${BUILD_SCRIPT_FILE}"

      sh """#!/bin/bash
        if [ -f "${WORKSPACE}/${BUILD_SCRIPT_FILE}" ] ; then
          /bin/sh '${WORKSPACE}/${BUILD_SCRIPT_FILE}'
        else
          echo "'${WORKSPACE}/${BUILD_SCRIPT_FILE}' does not exist!"
          exit 0
        fi
      """
    }
}
