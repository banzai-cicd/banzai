#!/usr/bin/env groovy

def call(config, msg, status) {

  githubNotify description: msg,  status: status, credentialsId: config.gitCredsId, account: config.gitAccount, gitApiUrl: GITHUB_API_URL

}
