#!/usr/bin/env groovy

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  env.GITHUB_API_URL = 'https://github.build.ge.com/api/v3'

  // if (config.debug) {
  //   println "ENVIRONMENT VARIABLES:"
  //   echo sh(returnStdout: true, script: 'env')
  // }

  /*
    Determine the total number of steps in the pipeline that are activated
    Jenkins Pipelines don't allow many groovy methods (CPS issues) like .findAll...hence the nastiness
  */
  def steps = []
  for (entry in [!config.skipSCM, config.sast, config.build, config.publish, config.deploy, config.integrationTests]) {
    if (entry == true) { steps.push(entry) }
  }
  def passedSteps = 0

  def passStep = { step ->
    passedSteps += 1
    println "BANZAI: ${step} PASSED : ${passedSteps}/${steps.size} STEPS COMPLETE"
    if (passedSteps >= steps.size) {
      currentBuild.result = 'SUCCESS'
    }
  }

  def isGithubError = { err ->
    return err.message.contains("The suplied credentials are invalid to login") ? true : false;
  }

  node() {

    sshagent (credentials: config.sshCreds) {
      // TODO notify Flowdock build starting
      echo "My branch is: ${BRANCH_NAME}"

      // checkout the branch that triggered the build if not explicitly skipped
      if (config.preCleanup) {
        println "Starting Fresh"
        step([$class: 'WsCleanup'])
      }

      if (!config.skipSCM) {
        try {
          notify(config, 'Checkout', 'Pending', 'PENDING')
          checkoutSCM(config)
          passStep('CHECKOUT')
          notify(config, 'Checkout', 'Successful', 'SUCCESS')
        } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'UNSTABLE'
          if (isGithubError(err)) {
            notify(config, 'Checkout', 'githubdown', 'FAILURE', true)
          } else {
            notify(config, 'Checkout', 'Failed', 'FAILURE')
          }
          throw err
        }
      }

      if (config.sast) {
        try {
          notify(config, 'SAST', 'Pending', 'PENDING')
          sast(config)
          passStep('SAST')
          notify(config, 'SAST', 'Successful', 'SUCCESS')
        } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'UNSTABLE'
          notify(config, 'Build', 'Failed', 'FAILURE')
          throw err
        }
      }

      if (config.build) {
        try {
          notify(config, 'Build', 'Pending', 'PENDING')
          build(config)
          passStep('BUILD')
          notify(config, 'Build', 'Successful', 'SUCCESS')
        } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          if (isGithubError(err)) {
            notify(config, 'Build', 'githubdown', 'FAILURE', true)
          } else {
            notify(config, 'Build', 'Failed', 'FAILURE')
          }
          throw err
        }
      }

      /*
        all notify calls past the build stage will skip notifcations to github
      */
      if (config.publish) {
        try {
          notify(config, 'Publish', 'Pending', 'PENDING', true)
          publish(config)
          passStep('PUBLISH')
          notify(config, 'Publish', 'Successful', 'SUCCESS', true)
        } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          if (isGithubError(err)) {
            notify(config, 'Publish', 'githubdown', 'FAILURE', true)
          } else {
            notify(config, 'Publish', 'Failed', 'FAILURE', true)
          }
          throw err
        }
      }

      if (config.deploy) {
        try {
          notify(config, 'Deploy', 'Pending', 'PENDING', true)
          deploy(config)
          passStep('DEPLOY')
          notify(config, 'Deploy', 'Successful', 'SUCCESS', true)
          // TODO notify Flowdock
        } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          notify(config, 'Deploy', 'Failed', 'FAILURE', true)
          throw err
        }
      }

      if (config.integrationTests) {
        try {
          notify(config, 'IT', 'Pending', 'PENDING', true)

          if (config.xvfb) {
            def screen = config.xvfbScreen ?: '1800x900x24';

            wrap([$class: 'Xvfb', screen: screen]) {
              integrationTests(config)
            }
          } else {
            integrationTests(config)
          }

          passStep('IT')
          notify(config, 'IT', 'Successful', 'SUCCESS', true)
        } catch (err) {
          echo "Caught: ${err}"
          currentBuild.result = 'FAILURE'
          notify(config, 'IT', 'Failed', 'FAILURE', true)
          throw err
        }
      }

      if (config.postCleanup) {
        println "Cleaning up"
        step([$class: 'WsCleanup'])
      }

    } // ssh-agent

  } // node

}
