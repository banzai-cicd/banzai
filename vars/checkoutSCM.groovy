#!/usr/bin/env groovy

def call(config) {

  stage('Checkout') {
    checkout scm
  }

}
