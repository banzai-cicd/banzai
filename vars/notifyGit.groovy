#!/usr/bin/env groovy

def call(config, msg, status) {

  githubNotify description: msg,  status: status, credentialsId: 'ge-git', account: 'ConfigReviewer', gitApiUrl: GITHUB_API_URL

}
