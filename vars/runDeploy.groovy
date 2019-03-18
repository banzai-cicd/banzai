#!/usr/bin/env groovy

def call(config) {

  /* Delpoy specific branches */
  if (config.deployCmd) {
    if (config.deploySSHCredId) {
      /* should wrap deployCmd in ssh creds */
      if (!config.deployUser) {
        logger "Deploy: No deployUser specified! When passing deploySSHCredId+deployCmd you must also pass deployUser. Skipping Deploy"
        return
      }
      if (!config.deployServer) {
        logger "Deploy: No deployServer specified! When passing deploySSHCredId you must also pass deployServer. Skipping Deploy"
        return
      }

      sh "ssh -o StrictHostKeyChecking=no ${config.deployUser}@${config.deployServer} '${config.deployCmd}'"
    } else {
      /* run deployCmd without ssh creds */
      sh "${config.deployCmd}"
    }
  } else {
    if (config.deploySSHCredId) {
      /* should wrap in ssh creds */
      sshagent([config.deploySSHCredId]) {
        runScript(config, "deployScriptFile", "deployScript")
      }
    } else {
      /* run without ssh creds */
      runScript(config, "deployScriptFile", "deployScript")
    }
  }

}
