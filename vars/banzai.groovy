#!/usr/bin/env groovy

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  env.GITHUB_API_URL = 'https://github.build.ge.com/api/v3'

  def steps = [config.sast, config.build, config.publish, config.deploy].findAll { it == true }
  def passedSteps = 0

  def passStep = { ->
    passedSteps += 1
    println "BANZAI: ${passedSteps}/${steps.size} STEPS PASSED"
    if (passedSteps >= steps.size) {
      currentBuild.result = 'SUCCESS'
    }
  }

  node() {
    // TODO notify Flowdock build starting
    echo "My branch is: ${BRANCH_NAME}"

    // checkout the branch that triggered the build if not explicitly skipped
    if (config.startFresh) {
      println "Starting Fresh"
      sh """#!/bin/bash
        rm -rf $WORKSPACE/*
      """
    }
    if (!config.skipSCM) {
      checkoutSCM(config)
    }

    if (config.sast) {
      try {
        notify(config, 'SAST', 'Pending', 'PENDING')
        sast(config)
        passStep()
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
        passStep()
        notify(config, 'Build', 'Successful', 'SUCCESS')
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notify(config, 'Build', 'Failed', 'FAILURE')
        // TODO notify Flowdock
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
        passStep()
        notify(config, 'Publish', 'Successful', 'SUCCESS', true)
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notify(config, 'Publish', 'Failed', 'FAILURE', true)
        // TODO notify Flowdock
        throw err
      }
    }

    if (config.deploy) {
      try {
        notify(config, 'Deploy', 'Pending', 'PENDING', true)
        deploy(config)
        passStep()
        notify(config, 'Deploy', 'Successful', 'SUCCESS', true)
        // TODO notify Flowdock
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notify(config, 'Deploy', 'Failed', 'FAILURE', true)
        throw err
      }
    }

  } // node

}
