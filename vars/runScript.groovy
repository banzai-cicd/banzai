#!/usr/bin/env groovy

import org.codehaus.groovy.runtime.MethodClosure;

def call(config, scriptPathOrClosure, args=null) {
    if (scriptPathOrClosure.getClass() == MethodClosure) {
      logger "Calling config MethodClosure"
      scriptPathOrClosure.call(config)
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

      sh "${fullPath} ${args ? args.join(' '): ''}"
    } else {
      error("User-provided scripts must be .sh scripts or Groovy method closures")
    }
  }
}
