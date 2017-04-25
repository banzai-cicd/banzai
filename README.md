![Banzai](https://www.dolphinsandyou.com/wp-content/uploads/2016/12/Banzai-pipeline.jpg =350x)

Banzai
========

```
banzai {
    appName = 'config-reviewer-server'
    gitCredId = 'sweeney-git'
    gitAccount = 'ConfigReviewer'
    developBranch = /^develop/
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
