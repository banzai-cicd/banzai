![Banzai](https://www.dolphinsandyou.com/wp-content/uploads/2016/12/Banzai-pipeline.jpg)

Banzai
========

example Jenkinsfile
```
banzai {
    appName = 'config-reviewer-server'
    gitCredId = 'sweeney-git'
    gitAccount = 'ConfigReviewer'
    sast = true
    sastBranches = /^tag\/\w*|develop/
    sastCredId = 'ge-checkmarx'
    build = true
    publish = true
    publishBranches = /^tag\/\w*|develop/
    deploy = true
    deployBranches = /^tag\/\w*|develop/
    deploySSHCredId = 'dev-ssh'
}
```


full list of Jenkins options
```
banzai {
    appName = 'config-reviewer-server'          // currently used only by SAST for determining the namespace to publish to.
    gitCredId = 'sweeney-git'                   // which credId in Jenkins to use for git.
    gitAccount = 'ConfigReviewer'               // the owner of the repo this pipeline is building for
    startFresh = true                           // wipe workspace before each build
    skipSCM = true                              // skip pulling down the branch that kicked off the build
    sast = true
    sastBranches = /^tag\/\w*|develop/          // regex to determine which branches to run SAST against
    sastCredId = 'ge-checkmarx'                 // which credId in Jenkins to use for SAST login
    build = true
    buildBranches = /^tag\/\w*|develop/         // regex to determine which branches to build
    buildScriptFile = 'buildScript.sh'          // location of buildScript. defaults to buildScript.sh
    publish = true
    publishBranches = /^tag\/\w*|develop/       // regex to determine which branches to publish
    publishScriptFile = 'publishScriptFile.sh'  // location of publishScript. defaults to publishScript.sh
    deploy = true
    deployBranches = /^tag\/\w*|develop/        // regex to determine which branches to deploy
    deployScriptFile = 'deployScript.sh'        // location of deployScript. defaults to deployScript.sh
    deploySSHCredId = 'dev-ssh'                 // if deploying over ssh, the credId in Jenkins to use for ssh
}
```
