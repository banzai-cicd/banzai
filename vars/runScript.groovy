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
    String returnData = sh(
      label: scriptPathOrClosure,
      returnStdout: true, 
      script: "${fullPath} ${args ? args.join(' '): ''}"
    )

    if (returnData.length() > 1 && returnData.contains('BanzaiUserData=')) {
      try {
          def userData = readJSON(text: returnData.tokenize('BanzaiUserData=')[1].trim())
          cfg.userData << userData
      } catch (Exception e) {
        logger "Unable to parse returned userData from ${scriptPathOrClosure}. Please ensure you're returning valid json"
      }
    }
  } else {
    error("User-provided scripts must be .sh scripts or Groovy method closures")
  }
}
