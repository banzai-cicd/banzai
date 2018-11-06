#!/usr/bin/env groovy

import org.codehaus.groovy.runtime.MethodClosure;

/*
  - configFilePropName - the config property used to specify a scriptFile
  - SCRIPT_DEFAULT - the default name of the script to look for if on isn't specified in the config
*/
def call(config, configFilePropName, SCRIPT_DEFAULT, args=null) {
  // test config for provided scriptFile ie) buildScriptFile = 'myBuildScript.sh'
  if(!config[configFilePropName]) {
    println "no ${configFilePropName} specified in config"
    // try and load defaults
    
    def SCRIPT_FILE = "${WORKSPACE}/${SCRIPT_DEFAULT}.sh"
    SCRIPT_FILE_STATUS = sh (
      script: 'if [ -e ${SCRIPT_FILE} ]; then echo "EXISTS" ; else echo "NOT-EXISTS" ; fi',
      returnStdout: true
    ).trim()
    echo "SCRIPT_FILE_STATUS: ${SCRIPT_FILE_STATUS}" 
    
    if (SCRIPT_FILE_STATUS == 'EXISTS') {
      println "${SCRIPT_DEFAULT}.sh detected"
      runShellScript("${SCRIPT_DEFAULT}.sh", args)
    } else {
      throw new IllegalArgumentException("no ${SCRIPT_DEFAULT}[.sh|.groovy] exists!")
    }
  } else {
    // A config can specify an .sh file OR pass a Groovy Closure
    println "${configFilePropName} detected in config"
    def SCRIPT_FILE = config[configFilePropName];

    if (SCRIPT_FILE.getClass() == MethodClosure) {
      println "Calling config MethodClosure"
      SCRIPT_FILE.call(config)
    } else if (SCRIPT_FILE.endsWith(".sh")) {
      println "Running ${config[configFilePropName]}"
      runShellScript(SCRIPT_FILE, args)
    } else {
      throw new IllegalArgumentException("${configFilePropName} must be of type .groovy or .sh")
    }
  }
}
