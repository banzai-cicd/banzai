![Banzai](http://i.imgur.com/QKdnoZ4.png)

Banzai
========

example Jenkinsfile
```
banzai {
    appName = 'config-reviewer-server'
    gitCredId = 'sweeney-git'
    gitAccount = 'ConfigReviewer'
    mergeBranches = /tag\-(.*)|develop/
    sast = true
    sastBranches = /tag\-(.*)|develop/
    sastCredId = 'ge-checkmarx'
    build = true
    publish = true
    publishBranches = /tag\-(.*)|develop/
    deploy = true
    deployBranches = /tag\-(.*)|develop/
    deploySSHCredId = 'dev-ssh'
}
```


full list of Jenkins options
```
banzai {
    appName = 'config-reviewer-server'          // **required** currently used only by SAST for determining the namespace to publish to.
    gitCredId = 'sweeney-git'                   // which credId in Jenkins to use for git.
    gitAccount = 'ConfigReviewer'               // the owner of the repo this pipeline is building for
    startFresh = true                           // wipe workspace before each build
    mergeBranches = /tag\-(.*)|develop/         // helps the pipeline dete
    skipSCM = true                              // skip pulling down the branch that kicked off the build
    flowdock = true
    flowdockCredId = 'flowdock-cred'
    flowdockAuthor = [
      name: 'Jenkins',
      avatar: 'https://github.build.ge.com/avatars/u/23999?s=466',
      email: 'Service.MyJenkins@ge.com'
    ]
    flowdockNotifyPRs = false                   // *default = false* whether or not to notify Flowdock with pr status changes
    sast = true
    sastBranches = /tag\-(.*)|develop/          // regex to determine which branches to run SAST against
    sastCredId = 'ge-checkmarx'                 // which credId in Jenkins to use for SAST login
    build = true
    buildBranches = /tag\-(.*)|develop/         // regex to determine which branches to build
    buildScriptFile = 'buildScript.sh'          // location of buildScript. defaults to buildScript.sh
    publish = true
    publishBranches = /tag\-(.*)|develop/       // regex to determine which branches to publish
    publishScriptFile = 'publishScriptFile.sh'  // location of publishScript. defaults to publishScript.sh
    deploy = true
    deployBranches = /tag\-(.*)|develop/        // regex to determine which branches to deploy
    deployScriptFile = 'deployScript.sh'        // location of deployScript. defaults to deployScript.sh
    deploySSHCredId = 'dev-ssh'                 // if deploying over ssh, the credId in Jenkins to use for ssh
}
```
