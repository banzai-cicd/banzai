![Banzai](https://i.imgur.com/QKdnoZ4.png)

Banzai
========

example Jenkinsfile
```
banzai {
    sshCreds = ['dev-ssh']
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
}
```


full list of Jenkins options
```
banzai {
    throttle = 'my-project'                     // comma-delimited list of throttle categories to apply. (https://github.com/jenkinsci/throttle-concurrent-builds-plugin)
    sshCreds                                    // a list of any ssh creds that may be needed in your pipeline
    appName = 'config-reviewer-server'          // **required** currently used only by SAST for determining the namespace to publish to.
    debug = false                               // provides additional debug messaging
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
    sastTeamUUID = 'fgdfg-5402-7635-4562-dfgfg' // SAST team id
    sastReportEmailTo = 'test@ge.com'           // optional. SAST report to be sent to which email id
    build = true
    buildBranches = /tag\-(.*)|develop/         // regex to determine which branches to build
    buildScriptFile = 'buildScript.sh'          // location of buildScript. defaults to buildScript.sh
    publish = true
    publishBranches = /tag\-(.*)|develop/       // regex to determine which branches to publish
    publishScriptFile = 'publishScriptFile.sh'  // location of publishScript. defaults to publishScript.sh
    deploy = true
    deployBranches = /tag\-(.*)|develop/        // regex to determine which branches to deploy
    deployScriptFile = 'deployScript.sh'        // location of deployScript. defaults to deployScript.sh
    jdk = 'jdk 10.0.1'                          // value must be the name given to a configured JDK in the Global Tools sections of Jenkins
    downstreamBuildBranches = /develop/
    downstreamBuilds = [
      develop: [
        [
          id: 'my-job',
          jobPath: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
          optional: true
        ]
      ]
    ]
}
```
