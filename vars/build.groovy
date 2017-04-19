#!/usr/bin/env groovy

import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.transform.Field
import static groovy.json.JsonOutput.*

def call(config) {
    def BUILD_SCRIPT_DEFAULT = 'buildScript'

    // now build, based on the configuration provided
    stage ('Build') {
      if (config.buildBranches) {
        def buildBranchesPattern = config.buildBranches
        Pattern pattern = Pattern.compile(buildBranchesPattern)

        if (!(BRANCH_NAME ==~ pattern)) {
          echo "${BRANCH_NAME} does not match the buildBranches pattern. Skipping Build"
          return
        }
      }

      //Variable Error Handeling
      if(!config.buildScriptFile) {
        println "no buildScript specified in config"
        // try and load defaults
        def groovyScript = new File("${WORKSPACE}/${BUILD_SCRIPT_DEFAULT}.groovy")
        if (groovyScript.exists()) {
          println "buildScript.groovy detected"
          runGroovyScript(groovyScript, config)
        } else {
          def shellScript = new File("${WORKSPACE}/./${BUILD_SCRIPT_DEFAULT}.sh")
          if (shellScript.exists()) {
            println "buildScript.sh detected"
            runShellScript(shellScript.getAbsolutePath())
          } else {
            throw new IllegalArgumentException("no buildScriptFile[.sh|.groovy] exists!")
          }
        }
      } else {
        println "buildScript detected in config"
        def BUILD_SCRIPT_FILE = config.buildScriptFile;
        if (BUILD_SCRIPT_FILE.endsWith(".sh")) {
          runShellScript(BUILD_SCRIPT_FILE)
        } else if (BUILD_SCRIPT_FILE.endsWith(".groovy")) {
          runGroovyScript(new File("${WORKSPACE}/${BUILD_SCRIPT_FILE}"), config)
        } else {
          throw new IllegalArgumentException("buildScriptFile must be of type .groovy or .sh")
        }
      }
    }
}

def runShellScript(BUILD_SCRIPT_FILE) {
  //Modify Variable to ensure path starts with "./"
  if(BUILD_SCRIPT_FILE.charAt(0) == "/"){
    BUILD_SCRIPT_FILE = "." + BUILD_SCRIPT_FILE
  }
  if(BUILD_SCRIPT_FILE.charAt(0) != "."){
    BUILD_SCRIPT_FILE = "./" + BUILD_SCRIPT_FILE
  }

  println "Running buildScript ${BUILD_SCRIPT_FILE}"
  println "Cmd: ${WORKSPACE}/${BUILD_SCRIPT_FILE} ${BRANCH_NAME}"

  sh """#!/bin/bash
    if [ -f "${WORKSPACE}/${BUILD_SCRIPT_FILE}" ] ; then
      /bin/bash ${WORKSPACE}/${BUILD_SCRIPT_FILE} ${BRANCH_NAME}
    else
      echo "'${WORKSPACE}/${BUILD_SCRIPT_FILE}' does not exist!"
      exit 0
    fi
  """
}

def runGroovyScript(BUILD_SCRIPT_FILE, config) {
  GroovyShell shell = new GroovyShell(this.getBinding());
  shell.evaluate(BUILD_SCRIPT_FILE);
}
