#!/usr/bin/env groovy
import com.ge.nola.BanzaiCfg
import org.codehaus.groovy.runtime.MethodClosure;

def call(BanzaiCfg cfg, scriptPathOrClosure, args=null) {
  if (scriptPathOrClosure instanceof MethodClosure) {
    logger "Calling cfg MethodClosure"
    scriptPathOrClosure.call(cfg)
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
    String returnData = sh(returnStdout: true, script: "${fullPath} ${args ? args.join(' '): ''}")
    if (returnData.length() > 1) {
      try {
          cfg.userData << readJSON text: returnData.trim()
      } catch (Exception e) {
        logger "Unable to parse returned userData from ${scriptPathOrClosure}. Please ensure you're returning valid json"
      }
    }
  } else {
    error("User-provided scripts must be .sh scripts or Groovy method closures")
  }
}
