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
    def shellScript = new File("${WORKSPACE}/./${SCRIPT_DEFAULT}.sh")
    if (shellScript.exists()) {
      println "${SCRIPT_DEFAULT}.sh detected"
      runShellScript(shellScript.name, args)
    } else {
      throw new IllegalArgumentException("no ${SCRIPT_DEFAULT}[.sh|.groovy] exists!")
    }
  } else {
    println "${configFilePropName} detected in config: ${config[configFilePropName]}"
    def SCRIPT_FILE = config[configFilePropName];

    println "SCRIPT_FILE.getClass(): ${SCRIPT_FILE.getClass()}"
    if (SCRIPT_FILE.getClass() == MethodClosure) {
      println "running closure"
      SCRIPT_FILE.call(config)
    } else if (SCRIPT_FILE.endsWith(".sh")) {
      println "running specific shell script"
      runShellScript(SCRIPT_FILE, args)
    } else {
      throw new IllegalArgumentException("${configFilePropName} must be of type .groovy or .sh")
    }
  }
}
