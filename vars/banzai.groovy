#!/usr/bin/env groovy

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  node {
    currentBuild.result = 'SUCCESS'
    echo "My branch is: ${BRANCH_NAME}"

    // checkout the branch that triggered the build
    checkoutSCM(config)
    // for some reason SCM marks PR as complete so we have to ovveride
    githubNotify description: 'Checkout Complete',  status: 'PENDING'

    if (config.sast) {
      try {
        sast(config)
        githubNotify description: 'SAST Complete',  status: 'PENDING'
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        throw err
      }
    }

    if (config.build) {
      try {
        build(config)
        githubNotify description: 'Build Complete',  status: 'PENDING'
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        throw err
      }
    }

    if (config.publish) {
      try {
        publish(config)
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        throw err
      }
    }

  } // node

}
