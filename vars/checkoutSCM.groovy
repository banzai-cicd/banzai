#!/usr/bin/env groovy

def call(config) {

  stage('Checkout') {
    // checkout scm

    // this is dumb but ge github non-ssh url access is failing.
    tokens = "${env.JOB_NAME}".tokenize('/')
    org = tokens[0]
    repo = tokens[1]
    branch = tokens[2]

    git branch: '${branch}',
      credentialsId: config.gitCredId,
      url: "ssh://git@github.build.ge.com:${org}/${repo}.git"
  }

}
