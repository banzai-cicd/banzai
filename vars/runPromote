#!/usr/bin/env groovy

banzai {
    appName = 'config-reviewer-service'
    jdk = 'jdk 10.0.1'
    sshCreds = ['dev-ssh', 'sweeney-git-ssh']
    gitCredId = 'sweeney-git'
    gitAccount = 'ConfigReviewer'
    flowdock = false
    flowdockAuthor = [
      name: 'Sweeney Jenkins',
      avatar: 'https://github.build.ge.com/avatars/u/23999?s=466',
      email: 'Service.SweeneyJenkins@ge.com'
    ]
    flowdockCredId = 'sweeney-flowtoken'
    mergeBranches = /tag\-(.*)|develop/
    sast = false
    sastBranches = /tag-(.*)|develop/
    sastCredId = 'ge-checkmarx'
    sastTeamUUID = '53f766d4-5402-4980-8a15-2655ba6d18f0'
    build = false
    publish = false
    publishBranches = /tag\-(.*)|develop/
    deploy = false
    deployBranches = /develop/
    promote = true
    promoteBranches = /tag\-(.*)/
    promoteRepo = 'git@github.build.ge.com:502061514/config-reviewer-deployment.git'
    stackName = config-reviewer
}
