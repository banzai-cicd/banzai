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
    def groovyScript = new File("${WORKSPACE}/${SCRIPT_DEFAULT}.groovy")
    if (groovyScript.exists()) {
      println "${SCRIPT_DEFAULT}.groovy detected"
      def ourSh = { String s -> sh(s) }
      runGroovyScript(groovyScript, config, ourSh)
    } else {
      def shellScript = new File("${WORKSPACE}/./${SCRIPT_DEFAULT}.sh")
      if (shellScript.exists()) {
        println "${SCRIPT_DEFAULT}.sh detected"
        runShellScript(shellScript.name, args)
      } else {
        throw new IllegalArgumentException("no ${SCRIPT_DEFAULT}[.sh|.groovy] exists!")
      }
    }
  } else {
    println "${configFilePropName} detected in config: ${config[configFilePropName]}"
    def SCRIPT_FILE = config[configFilePropName];

    if (SCRIPT_FILE.getClass() == MethodClosure) {
      SCRIPT_FILE.call(config)
    } else if (SCRIPT_FILE.endsWith(".sh")) {
      runShellScript(SCRIPT_FILE, args)
    // } else if (SCRIPT_FILE.endsWith(".groovy")) {
    //   def ourSh = { String s -> sh(s) }
    //   runGroovyScript(new File("${WORKSPACE}/${SCRIPT_FILE}"), config, ourSh)
    } else {
      throw new IllegalArgumentException("${configFilePropName} must be of type .groovy or .sh")
    }
  }
}
