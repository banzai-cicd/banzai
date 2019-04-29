#!/usr/bin/env groovy

def call(config) {

  if (config.publish) {
    if (config.publishBranches && !(BRANCH_NAME ==~ config.publishBranches)) {
      logger "${BRANCH_NAME} does not match the publishBranches pattern. Skipping"
      return
    }

    stage ('Publish') {
      try {
        notify(config, 'Publish', 'Pending', 'PENDING', true)
        publish(config)
        notify(config, 'Publish', 'Successful', 'PENDING', true)
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        if (isGithubError(err)) {
          notify(config, 'Publish', 'githubdown', 'FAILURE', true)
        } else {
          notify(config, 'Publish', 'Failed', 'FAILURE', true)
        }

        error(err.message)
      }
    }
  }

}
