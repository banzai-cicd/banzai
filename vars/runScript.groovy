#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import org.jenkinsci.plugins.workflow.cps.CpsClosure2

def call(BanzaiCfg cfg, scriptPathOrClosure, List args=null) {
  // if we have userData
  if (!cfg.userData.isEmpty()){
    if (args == null) { args = [] }
    // in the event that there is userData, it should always be passed as the 1st arg
    // so that users can be 
    args.add(0, cfg.userData)
  }

  if (scriptPathOrClosure instanceof org.jenkinsci.plugins.workflow.cps.CpsClosure2) {
    scriptPathOrClosure.call(args)
  } else if (scriptPathOrClosure.endsWith(".sh")) {
    if (scriptPathOrClosure.charAt(0) == "/"){
      scriptPathOrClosure = "." + scriptPathOrClosure
    }
    if (scriptPathOrClosure.charAt(0) != "."){
      scriptPathOrClosure = "./" + scriptPathOrClosure
    }

    String fullPath = "${WORKSPACE}/${scriptPathOrClosure}"

    if (!fileExists(fullPath)) {
      logger "'${scriptPathOrClosure}' does not exist in the workspace!"
      return
    }

    // run the bashScript
    sh(
      label: scriptPathOrClosure,
      script: "${fullPath} ${args ? args.join(' '): ''}"
    )

    def userData
    def userDataFileName = "${WORKSPACE}/BanzaiUserData"
    try {
      if (fileExists("${userDataFileName}.yaml")) {
        userData = readYaml file: "${userDataFileName}.yaml"
        new File("${userDataFileName}.yaml").remove()
      } else if (fileExists("${userDataFileName}.json")) {
        userData = readJSON file: "${userDataFileName}.json"
        new File("${userDataFileName}.json").remove()
      }
      cfg.userData << userData
    } catch (Exception e) {
      logger "Unable to parse BanzaiUserData. Please ensure you're writing valid json or yaml"
      logger e.message
    }
  } else {
    error("User-provided scripts must be .sh scripts or Groovy method closures")
  }
}
