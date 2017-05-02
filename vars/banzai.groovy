#!/usr/bin/env groovy

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  env.GITHUB_API_URL = 'https://github.build.ge.com/api/v3'

  node() {
    // TODO notify Flowdock build starting
    notifyFlowdock2(config, 'Testing', 'PENDING')

    currentBuild.result = 'SUCCESS'
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
        notifyGit(config, 'SAST Pending', 'PENDING')
        sast(config)
        notifyGit(config, 'SAST Complete', 'SUCCESS')
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'UNSTABLE'
        notifyGit(config, 'Build Failure', 'ERROR')
        throw err
      }
    }

    if (config.build) {
      try {
        notifyGit(config, 'Build Pending', 'PENDING')
        build(config)
        notifyGit(config, 'Build Complete', 'SUCCESS')
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        notifyGit(config, 'Build Failure', 'FAILURE')
        // TODO notify Flowdock
        throw err
      }
    }

    if (config.publish) {
      try {
        publish(config)
        // TODO notify Flowdock
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        // TODO notify Flowdock
        throw err
      }
    }

    if (config.deploy) {
      try {
        deploy(config)
        // TODO notify Flowdock
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        // TODO notify Flowdock
        throw err
      }
    }

  } // node

}
