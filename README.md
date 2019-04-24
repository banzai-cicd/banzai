![Banzai](https://i.imgur.com/QKdnoZ4.png)

Banzai
========

example Jenkinsfile
```
banzai {
    sshCreds = ['dev-ssh']
    appName = 'config-reviewer-server'
    gitTokenId = 'sweeney-git'
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
    gitTokenId = 'sweeney-git-token'            // a Jenkins credential id which points to a github token (required by downstreamBuilds)
    startFresh = true                           // wipe workspace before each build
    mergeBranches = /tag\-(.*)|develop/         // helps the pipeline dete
    skipSCM = true                              // skip pulling down the branch that kicked off the build
    flowdock = true                             // 
    flowdockCredId = 'flowdock-cred'
    flowdockAuthor = [
      name: 'Jenkins',
      avatar: 'https://github.build.ge.com/avatars/u/23999?s=466',
      email: 'Service.MyJenkins@ge.com'
    ]
    flowdockNotifyPRs = false                   // *default = false* whether or not to notify Flowdock with pr status changes
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
    vulnerabilityAbortOnError                 // globally set that all vulnerability scans should abort the pipeline if there is an Error
    vulnerabilityScans = [
      /develop/: [                              // run this collection of scans against develop
        [
          type: 'checkmarx',
          credId: 'ge-checkmarx',               // jenkins credential containing user/pass for checkmarx
          resultEmails: ['your_email@ge.com'],
          preset: '17',                         // defaults to '17'
          teamUUID: 'your-checkmarx-team-uuid',
          abortOnError: false                 // determines of this scan should cause the pipeline to abort if it results in an Error.
        ],
        [
          type: 'coverity',
          credId: 'coverity-auth-key-file',   // jenkins credId of type 'file' representing your users authentication key (exported from coverity server UI)
          toolId: 'coverity-2018.12',         // the id given to the Jenkins Global Tool installation for Coverity
          serverHost: 'coverity.power.ge.com',
          serverPort: '443',
          resultEmails: ['simon.townsend1@ge.com'],
          buildCmd: 'mvn -s ./settings.xml clean install -U', // the command coverity should wrap. alternatively, you can export BUILD_CMD in a previous pipeline step and it will be picked up.
          projectName: 'your-coverity-project-name',
          abortOnError: true
        ]
      ]
    ]
    downstreamBuilds = [
      /develop/: [ // 'develop' signifies that this collection of downstream build definition's will only run when the 'develop' branch is matched
        [
          id: 'my-job',
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
          optional: true // when true, the downstream build will only run if the Pull Request contains a label in the format 'build:<job-id>', ie) 'build:my-job'
        ],
        [
          id: 'my-parallel-job',
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
          parallel: true
        ],
        [
          id: 'my-parallel-job-2',
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
          parallel: true,
          propagate: true // this would mark 'my-job' as failed if 'my-parallel-job-2' fails
        ],
        [
          id: 'my-serial-job', // this build would run in serial AFTER the 2 parallel builds complete
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch'
        ]
      ]
    ]
    filterSecrets = [
      /develop/: [
          file: 'settings.xml',                     // the filePath to filter relative to the jenkins WORKSPACE root
          label: 'myPass',                          // should appear in the file in the format ${banzai:myPass}
          secretId: 'my-jenkins-secret-id'          // the id of the secret on Jenkins
      ]
    ]
}
```

### downstreamBuilds
*Note: downstreamBuilds requires that `gitTokenId` is defined*
The downstream build definition supports all of the properties documented by [Jenkins Pipeline Build Step](https://jenkins.io/doc/pipeline/steps/pipeline-build-step/) as well as 3 custom properties `id`, `optional` and `parallel`. `id` is used to map Github PR labels in the event that `optional` is set to `true`. When a build completes and the next build(s) have `parallel: true` then it will by default start those builds but will not wait for them to complete. There are 3 scenarios where the build will wait for parallel builds to complete:
1. the parallel build also has `wait: true`
2. the parallel build also has `propagate: true`
3. there is one or more non-parallel builds defined after the parallel build(s) that need to be executed once the parallel build(s) complete.
