#!/usr/bin/env groovy

def call(config, stage, msg, status) {
  //githubNotify description: msg,  status: status, credentialsId: config.gitTokenId, account: config.gitAccount, gitApiUrl: GITHUB_API_URL
  // infer credentials and account from build info
  githubNotify description: msg,  context: "Banzai:${stage}", status: status, gitApiUrl: GITHUB_API_URL
}