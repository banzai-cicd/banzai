![Banzai](https://i.imgur.com/QKdnoZ4.png)

Banzai
========
Banzai started as one team's solution to CICD and has grown to a full-featured CICD offering. All of Banazai's features are implemented with genericity and flexibility in mind so if something does not meet your needs please open a Git issue and let us know!

* [Configuration Overview](#configuration-overview)
* [BanzaiCfg](#banzaicfg)
  * [appName](#appName)
  * [sshCreds](#sshCreds)
  * [timeout](#timeout)
  * [throttle](#throttle)
  * [filterSecrets](#filterSecrets)
  * [skipScm](#skipSCM)
  * [debug](#debug)
  * [gitTokenId](#gitTokenId)
  * [httpProxy and httpsProxy](#httpProxy-and-httpsProxy)
  * [cleanWorkspace](#cleanworkspace)
  * [build](#build)
  * [publish](#publish)
  * [deploy](#deploy)
  * [itegrationTests](#integrationTests)
  * [tools](#tools)
  * [notifications](#notifications)
  * [vulnerabilityScans](#vulnerabilityScans)
  * [vulnerabilityAbortOnError](#vulnerabilityAbortOnError)
  * [qualityScans](#qualityScans)
  * [qualityAbortOnError](#qualityAbortOnError)
  * [downstreamBuilds](#downstreamBuilds)
  * [powerDevOpsReporting](#powerDevOpsReporting)
  * [gitOpsTrigger and gitOps](#gitopstrigger-and-gitops)
    * [GitOps Configuration](#gitops-configuration)
  * [stages](#stages)
* [Coverity](#coverity)
* [BanzaiUserData](#banzaiuserdata)

## Configuration Overview
Basic Jenkinsfile
```
banzai([
  appName: 'my-app',
  build: [ /.*/: [:] ],                                                  // build all branches (calls build.sh)
  publish: [ /tag\-(.*)|develop/: [script : 'scripts/my-publish.sh'] ],  // publish tags and develop branch changes. Execute scripts/my-publish.sh instead of the default publish.sh
  deploy: [ /tag\-(.*)|develop/: [:] ],                                  // deploy tags and develop branch changes (calls deploy.sh)
])
```

Exhaustive List of BanzaiCfg properties
```
@Library('Banzai@1.0.0') _ // only necessary if configured as a 'Global Pipeline Library'. IMPORTANT: the _ is required after @Library. 
banzai([
    appName: 'config-reviewer-server',
    sshCreds: ['cred1', 'cred2'],
    timeout: 30,
    throttle: ['my-project'],
    filterSecrets: [
      /develop/: [
          file: 'settings.xml',
          label: 'myPass',
          secretId: 'my-jenkins-secret-id'
      ]
    ],
    skipSCM: true,
    debug: false,
    gitTokenId: 'sweeney-git-token',
    httpsProxy: [
      envVar: 'https_proxy',                    // optionally use an env variable from the host to set the proxy info. should be in "{host}:{port}" format.
      host: 'proxyhost',
      port: '80'
    ],
    cleanWorkspace: [
      pre: true,
      post: true
    ],
    build: [
      /.*/ : [:]
    ],
    publish: [
      /.*/ : [
        shell: 'scripts/my-publish-script.sh'
      ]
    ],
    deploy: [
      /.*/ : [
        shell: 'scripts/my-deploy-script.sh'
      ]
    ],
    integrationTests: [
      /.*/ : [
        shell: 'scripts/my-it-script.sh'
      ]
    ],
    tools: [
      jdk: 'jdk 10.0.1',
      node: '8'
    ],
    flowdock: [
      banzaiFlow: [
        credId: 'banzai-flowtoken',
        author: [
          name: 'Banzai',
          avatarUrl: 'https://github.com/avatars/u/55576?s=400&u=700c7e70356d1f5a679908c1d7c7e5bf8e2beab6',
          email: 'banzai@banzai.com'
        ]
      ]
    ],
    email: [
      addresses: [
        tom: 'tom@jerry.com',
        banzai: 'banzai@banzai.com'
      ],
      groups: [
        everyone: ['tom', 'banzai'],
      ]
    ],
    notifications: [
      flowdock: [
        /.*/: [
          'banzaiFlow': ['.*']
        ]
      ],
      email: [
        /.*/: [
          groups: [
            'everyone': ['PIPELINE:(FAILURE|SUCCESS)']
          ],
          individuals: [
            'tom': ['PIPELINE:PENDING']
          ]
        ]
      ]
    ],
    vulnerabilityScans = [
      /develop|master/: [
        [
          type: 'checkmarx',
          credId: 'ge-checkmarx',
          resultEmails: ['your_email@ge.com'],
          preset: '17',
          teamUUID: 'your-checkmarx-team-uuid',
          abortOnError: false
        ],
        [
          type: 'coverity',
          credId: 'coverity-auth-key-file',
          toolId: 'coverity-2018.12',
          serverHost: 'coverity.power.ge.com',
          serverPort: '443',
          resultEmails: ['simon.townsend1@ge.com'],
          buildCmd: 'mvn -s ./settings.xml clean install -U',
          projectName: 'your-coverity-project-name',
          abortOnError: true
        ]
      ]
    ],
    vulnerabilityAbortOnError = false,
    qualityScans: [
      /develop|master/: [
        [
          type: 'sonar',
          serverUrl: 'https://my-sonar.ge.com'
          credId: 'sonar-auth-token-id'
        ]
      ]
    ],
    qualityAbortOnError = false,
    downstreamBuilds: [
      /develop/: [
        [
          id: 'my-job',
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
          optional: true,
          wait: true
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
          propagate: true
        ],
        [
          id: 'my-serial-job',
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch'
        ]
      ]
    ],
    powerDevOpsReporting: [
      branches: /master|develop/
      ci: 'your-ci',
      uai: 'your-uai',
      uaaCredId: 'uaa-cred-id',                     // UAA Bearer Token stored as Jenkins Cred
      uaaUrl: 'https://a8a2ffc4-b04e-4ec1-bfed-7a51dd408725.predix-uaa.run.aws-usw02-pr.ice.predix.io/oauth/token?grant_type=client_credentials',
      metricsUrl: 'https://dev-cicadavpc-secure-pipeline-services-vanguard.cicada.digital.ge.com',
      environments: [
        /develop/ : [
          key: 0,
          name: 'develop'
        ],
        /master/ : [
          key: 1,
          name: 'qa'
        ]
      ]
    ],
    gitOpsTrigger: [
      /develop|tag-*/ : [
        jenkinsJob: '/Banzai/GitOps/master',
        stackId: 'dib'
      ]
    ],
    gitOps: [
      autoDeploy: [
        /develop/ : 'dev'
      ],
      envs: [
        'dev' : [:],
        'qa' : [
            approvers: ['<jenkins-id>'],
            watchers: ['<jenkins-id>']
        ]
      ]
    ],
    stages: [
      [ name: 'build' ],
      [
        name: 'My Arbitrary Stage',
        steps: [
          /.*/: [
            [
              groovy: { logger "YO I RAN A CLOSURE FROM A CUSTOM STAGE!" }
            ], 
            [
              shell: 'customStageScript.sh'
            ]
          ]
        ]
      ]
    ]
  ]
])
```

## BanzaiCfg
The BanzaiCfg is the object passed to the `banzai()` entrypoint in your Jenkinsfile. The Map that you pass in is mapped to typed BanzaiCfg objects. The specific typed objects are referenced throughout the following documentation.

### appName
**String** <span style="color:red">*</span>  
Used throughout the pipeline in various contexts to indentify the name of the app/service/lib/etc being processed.

### sshCreds
**List\<String>**  
A list of id's that map to Jenkins Credentials of type 'SSH Username with private key'. When configured, the ssh credentials will be available for the duration of the pipeline run.

### timeout
**int** *default: 30*  
Time in minutes the pipeline must complete with-in before being aborted

### throttle
**List\<String>**  
The `throttle` property leverages the [Throttle Concurrent Builds Plugin](https://github.com/jenkinsci/throttle-concurrent-builds-plugin) to provide a way of restricting the number of concurrent builds belonging to a particular 'throttle category' at any given time. This is useful when you have builds that require a large number of resources.
1. Install the [Throttle Concurrent Builds Plugin](https://github.com/jenkinsci/throttle-concurrent-builds-plugin), 
2. Configure the plugin (create a throttle group)
3. Update your BanzaiCfg
```
throttle = ['my-throttle-group']
```

### filterSecrets
**[BanzaiFilterSecretsCfg](src/com/ge/nola/cfg/BanzaiFilterSecretsCfg.groovy)**  
If your pipeline requires secret values exist in files but you do not want to store them in version control, `filterSecrets` can help.

1. Add a credential to Jenkins of type 'secret'. (remember the id)
2. Update the file in your repository that you would like to have the secret injected into.
```
properties:
  username: 'myuser'
  password: '${banzai:myPass}'
```
3. Update the BanzaiCfg to include the `filterSecrets` property
```
filterSecrets: [
  /develop/: [
      file: 'settings.xml',   // the filePath to filter relative to the jenkins WORKSPACE root
      label: 'myPass',        // should appear in the file in the format ${banzai:myPass}
      secretId: 'my-secret'   // the id of the secret on Jenkins
  ]
]
```

### skipSCM
**Boolean** <i>default: false</i>  
When true, will skip cloning down the repsitory which triggered the pipeline.

### debug
**Boolean** <i>default: false</i>  
When true, adds additional logs to Jenkins console

### gitTokenId
**String** <span style="color:red">*</span>  
The id of a Jenkins Credential of type 'secret' containing a Github Personal Access Token. Currently used for updating PR statuses and by the [downstreamBuilds](#downstreamBuilds) feature. The token must include at a minimum the entire `repo` scope. 

### httpProxy and httpsProxy
**[BanzaiProxyCfg](src/com/ge/nola/cfg/BanzaiProxyCfg.groovy)**  
If Jenkins is deployed behind a firewall it's a good idea to set the `httpProxy` and `httpsProxy`. If you have an ENV var set in your Jenkins environment such as `HTTP_PROXY` that you would like to inherit from. Set the `envVar` property of the [BanzaiProxyCfg](src/com/ge/nola/cfg/BanzaiProxyCfg.groovy) equal to the name of that ENV var.

### cleanWorkspace
**[BanzaiCleanWorkspaceCfg](src/com/ge/nola/cfg/BanzaiCleanWorkspaceCfg.groovy)**  
Ensure a cleaned WORKSPACE prior to pipeline run or clean up the WORKSPACE after a pipeline run.  
ex)
```
cleanWorkspace : [
  pre: true,
  post: true
]
```

### build
**Map<String,[BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy)>**  
Configures the built-in 'Build' stage of Banzai. The config is branch-based meaning that the keys of the supplied Map should be regex patterns matching the branch that each [BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy) should apply to. To accept the defaults pass an empty object (`[:]`) as your [BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy).
ex)
```
build: [
  /.*/ : [:]  // defaults to [ shell: 'build.sh' ]
],
```

### publish
**Map<String,[BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy)>**  
Configures the built-in 'Publish' stage of Banzai. The config is branch-based meaning that the keys of the supplied Map should be regex patterns matching the branch that each [BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy) should apply to. To accept the defaults pass an empty object (`[:]`) as your [BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy).
ex)
```
publish: [
  /.*/ : [:]  // defaults to [ shell: 'publish.sh' ]
],
```

### deploy
**Map<String,[BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy)>**  
Configures the built-in 'Deploy' stage of Banzai. The config is branch-based meaning that the keys of the supplied Map should be regex patterns matching the branch that each [BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy) should apply to. To accept the defaults pass an empty object (`[:]`) as your [BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy).
ex)
```
deploy: [
  /.*/ : [:]  // defaults to [ shell: 'deploy.sh' ]
],
```

### integrationTests
**Map<String,[BanzaiIntegrationTestsCfg](src/com/ge/nola/cfg/BanzaiIntegrationTestsCfg.groovy)>**  
Extends the [BanzaiStepCfg](src/com/ge/nola/cfg/BanzaiStepCfg.groovy) and adds additional properties for `xvfb` and `xvfbScreen`. *note xvfb features require that the [Xvfb Plugin](https://wiki.jenkins.io/display/JENKINS/Xvfb+Plugin) is installed on the Jenkins instance.*

### tools
**[BanzaiToolsCfg](src/com/ge/nola/cfg/BanzaiToolsCfg.groovy)**  
The `tools` property allows you to target specific items from your Jenkins Global Tool Configuration ie) `jdk`, `nodejs`. Tools configured via the [BanzaiToolsCfg](src/com/ge/nola/cfg/BanzaiToolsCfg.groovy) object will be in scope for the duration of the pipeline run. 

### node
If there are multiple Node versions configured in Jenkins Global Tool Configuration and the id is provided to the `node` property it will be used for the duration of the pipeline run.

### notifications
**[BanzaiNotificationsCfg](src/com/ge/nola/cfg/BanzaiNotificationsCfg.groovy)**  
Determines when/how notifications are sent and who recieves those notifications. the `notifications` property works in tandem with the [email](src/com/ge/nola/cfg/BanzaiEmailCfg.groovy) and [flowdock](/src/com/ge/nola/cfg/BanzaiFlowdockCfg.groovy) properties
ex)
```
flowdock: [
  banzaiFlow: [
    credId: 'banzai-flowtoken',
    author: [
      name: 'Banzai',
      avatarUrl: 'https://github.com/avatars/u/55576?s=400&u=700c7e70356d1f5a679908c1d7c7e5bf8e2beab6',
      email: 'banzai@banzai.com'
    ]
  ]
],
email: [
  addresses: [
    tom: 'tom@jerry.com',
    banzai: 'banzai@banzai.com'
  ],
  groups: [
    everyone: ['tom', 'banzai'],
  ]
],
notifications: [
  flowdock: [
    /.*/: [ // this config applies to all branches
      'banzaiFlow': ['.*'] // all branches will use the flowdock 'banzaiFlow' config and publish all notification events.
    ]
  ],
  email: [
    /.*/: [ // this config applies to all branches
      groups: [
        'everyone': ['PIPELINE:(FAILURE|SUCCESS)'] // everyone will get an email for pipeline success and failure
      ],
      individuals: [
        'tom': ['PIPELINE:PENDING'] // only tom will get emails about pending pipelines
      ]
    ]
  ]
]
```

### vulnerabilityScans
**Map<String, List<[BanzaiVulnerabilityCfg](src/com/ge/nola/cfg/BanzaiVulnerabilityCfg.groovy)>>**  
Banzai supports `checkmarx` and `coverity` Vulnerability Scans. The config is branch-based meaning that the keys of the supplied Map should be regex patterns matching the branch that each [BanzaiVulnerabilityCfg](src/com/ge/nola/cfg/BanzaiVulnerabilityCfg.groovy) should apply to.
ex)
```
vulnerabilityScans = [
  /develop|master/: [
    [
      type: 'checkmarx',
      credId: 'ge-checkmarx',               // jenkins credential containing user/pass for checkmarx
      resultEmails: ['your.email@email.com'],
      preset: '17',                         // defaults to '17'
      teamUUID: 'your-checkmarx-team-uuid'
    ],
    [
      type: 'coverity',
      credId: 'coverity-auth-key-file',     // jenkins credId of type 'file' representing your users authentication key (exported from coverity server UI)
      toolId: 'coverity-2018.12',           // the id given to Coverity i the Jenkins Global Tool installation
      serverHost: 'coverity.power.ge.com',
      serverPort: '443',
      resultEmails: ['your.email@email.com'],
      buildCmd: 'mvn -s ./settings.xml clean install -U', // the build command coverity should wrap. alternatively, you can leverage banzai BanzaiUserData. see BanzaiUserData section of README
      projectName: 'your-coverity-project-name'
    ]
  ]
]
```

### vulnerabilityAbortOnError
**Boolean**  
Aborts the pipeline if any of the Vulnerability Scans throw an error during execution

### qualityScans
**Map<String, List<[BanzaiQualityCfg](src/com/ge/nola/cfg/BanzaiQualityCfg.groovy)>>**  
Banzai supports `sonar` Quality Scans. The config is branch-based meaning that the keys of the supplied Map should be regex patterns matching the branch that each [BanzaiQualityCfg](src/com/ge/nola/cfg/BanzaiQualityCfg.groovy) should apply to.
ex)
```
qualityScans: [
  /develop|master/: [
    [
      type: 'sonar',
      serverUrl: 'https://my-sonar.ge.com'
      credId: 'sonar-auth-token-id'        // jenkins credential (of type secret) containing a sonar server auth token
    ]
  ]
]
```

### qualityAbortOnError
**Boolean**  
Aborts the pipeline if any of the Quality Scans throw an error during execution

### downstreamBuilds
**Map<String, List<[BanzaiDownstreamBuildCfg](src/com/ge/nola/cfg/BanzaiDownstreamBuildCfg.groovy)>>**  
Banzai allows you to execute additional builds on completion of a pipeline. These 'Downstream Builds' can be optional or required. In the event that they are required they will always run upon success of a build. If they are marked as 'optional' the Pull Request must contain a label in the format `build:<buildId>`. 
ex) running a downstream job where the parent job depends on the result.
```
downstreamBuilds: [
  /develop/: [
    [
      id: 'my-job',
      job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
      propagate: true
    ]
  }
```
#### Parallel Downsteam Build Behavior
When Banzai encounters 1 or more BanzaiDownstreamBuildCfg's that contain `parallel: true` listed sequentially, Banzai will execute those builds in parallel and not wait for them to complete. There are 3 scenarios where the parent build will wait for parallel builds to complete:
1. the parallel build also has `wait: true`
2. the parallel build also has `propagate: true`
3. there is one or more non-parallel builds defined after the parallel build(s) that need to be executed once the parallel build(s) complete.
ex)
The following example is configured as follows
1. 'my-job' would only run if the pull-request that kicked off the parent job included the label 'build:my-job' because `optional: true`. If it runs it would block the parent job until it completes because `wait: true`.
2. 'my-parallel-job' and 'my-parallel-job-2' would execute in parallel. 'my-parallel-job-2' includes `propagate: true` and therefor blocks the parent job as it depends on the result of 'my-parallel-job-2'
3. 'my-serial-job' would run after both parallel jobs complete.
```
downstreamBuilds: [
  /develop/: [
    [
      id: 'my-job',
      job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
      optional: true,
      wait: true
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
      propagate: true
    ],
    [
      id: 'my-serial-job',
      job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch'
    ]
  ]
  ],
```

*Note: Downstream Builds requires that `gitTokenId` is defined*

### powerDevOpsReporting
**[BanzaiDevOpsReportingCfg](src/com/ge/nola/cfg/BanzaiDevOpsReportingCfg.groovy)**  
Banzai is full integrated with the Power DevOps Reporting Dashboard. By providing the following configuration information about your pipeline run and any scans executed will be sent automatically to the Dashboard  
ex)
```
powerDevOpsReporting: [
  branches: /master|develop/
  ci: 'your-ci',
  uai: 'your-uai',
  uaaCredId: 'uaa-cred-id', // Jenkins Cred stored as a 'username and password' where the password is a UAA Bearer Token
  uaaUrl: 'https://a8a2ffc4-b04e-4ec1-bfed-7a51dd408725.predix-uaa.run.aws-usw02-pr.ice.predix.io/oauth/token?grant_type=client_credentials',
  metricsUrl: 'https://prod-cicadavpc-secure-pipeline-services-vanguard.cicada.digital.ge.com',
  environments: [
    /develop/ : [
      key: 0,
      name: 'develop'
    ],
    /master/ : [
      key: 1,
      name: 'qa'
    ]
  ]
]
```

### gitOpsTrigger and gitOps
****
Banzai supports [GitOps-style](https://www.xenonstack.com/insights/what-is-gitops/) deployments. GitOps allows you to back your environments and handle their deployments from a single repository as well as decouple CI from CD (good for security). Once configured, your Service repositories will complete all CI Banzai Pipeline steps. If Banzai determines that a new version has been created or a deployment should take place it will trigger a new Banzai Pipeline that builds your GitOps repository. The GitOps repository is responsible for recording all versions of each Service and updating your 'Stacks' with the correct versions in each environment. [You can view an example GitOps repo here](https://github.build.ge.com/Banzai-CICD/GitOps). For our purposes, a 'Stack' is merely a collection of indvidual Services that should be deployed together.

There are 2 methods of deployment via Banzai GitOps.
1. automatic deployment
  - the .banzai of your GitOps repo can be configured to automatically trigger a deployment based on the git branch of the upstream Service that triggered the GitOps job
2. manual deployment
  1. A user manually runs 'Build With Parameters' on the GitOps master job manually from with-in Jenkins. 
  2. The user will be presented with a series of user-input stages.
  3. The user can choose between 3 different styles of deployment
    1. 'Select Specific Versions' - The user will provide the version for each Service that should be deployed
    2. 'Promote Stack From Another Environment' - The versions from one environment will be copied to another
    3. 'Rollback Stack' - select a previous deployment to re-assign to an environment

#### GitOps Configuration

1. update the .banzai file in the repository of each Service that you would like to trigger a GitOps job with an instance of **Map<String, [BanzaiGitOpsTriggerCfg](src/com/ge/nola/cfg/BanzaiGitOpsTriggerCfg.groovy)>**  

ex)
```
# ensure that 'deploy' is removed then add:

gitOpsTrigger: [
  /develop|tag-*/ : [
    jenkinsJob: '/Banzai/GitOps/master', # the path to the GitOps master job in jenkins
    stackId: 'dib'                       # the GitOps 'stack' that this service is a member of
  ]
]
```
2. At some point during your pipeline, write a `BanzaiUserData.[yaml/json]` to the root of your project WORKSPACE like the following
```
"gitOps": {
  "versions": {
      "test-maven" : {          // add an entry for each service in a GitOps stack that should update its version
          "version": "1.0.0",
          "meta": {
              "some": "example meta"
          }
      }
  }
}
```
This information will be passed to your GitOps pipeline so that it is aware of what services should be updated

3. Create a GitOps repo. You can use the [GitOps-starter](https://github.build.ge.com/Banzai-CICD/GitOps-starter) to speed things up.  
**Your GitOps Repo Must Contain**  
- `envs` - directory with sub-directories for each environment
- `services` - directory (this is where the available versions of each service will be stored)
- `.banzai` - file with a `gitOps` section
- `deployScript.sh` - will be called for each deployment as passed arguments containing the stack and service versions to deploy

3a. ensure your .banzai file in the GitOps repo includes an instance of **[BanzaiGitOpsCfg](src/com/ge/nola/cfg/BanzaiGitOpsCfg.groovy)**  
ex)
```
gitOps: [
    autoDeploy: [
        /develop/ : 'dev'
    ],
    envs: [
        'dev' : [:],
        'qa' : [
            approvers: ['212589146'],
            watchers: ['212589146']
        ]
    ]
]
```

### stages
**List<[BanzaiStageCfg](src/com/ge/nola/cfg/BanzaiStageCfg.groovy)>**  
If the supplied build,publish,deploy,integrationTests do not satisfy your pipeline needs you can compose your own steps via the `stages` BanzaiCfg property. The `stages` property can contain a list of custom steps or a mixture of custom stages and Banzai-provided stages (such as build,publish,etc). Because of this, the `name` property of the [BanazaiStageCfg](src/com/ge/nola/cfg/BanzaiStageCfg.groovy) is reserved for `build|deploy|publish|integrationTests|scans:vulnerability|scans:quality`.  

Banzai will perform basic functions such as notifications and error handling for you during your custom stage execution. Be aware, Stages and boilerplate that exist for supporting SCM, Power DevOps Reporting, Secrets Filtering, GitOps, proxy etc will still evaluate. ie) when you leverage the `stages` property you will only be overriding the following Stages `vulnerabilityScans, qualityScans, build, publish, deploy, integrationTests`  

ex) the following example contains a mix of Banzai-provided Stages and User-provided Stages. Note, when calling a Banzai-provided stage you should still configure that stage using it's existing BanzaiCfg property.
```
build: [
      /.*/ : [ shell: 'scripts/build.sh' ]
],
stages: [
  [ name: 'build' ],                  // call existing Banzai-provided build stage
  [
    name: 'My Arbitrary Stage',       // provide a custom Banzai Stage name
    steps: [
      /.*/: [                         // steps for a custom stage can be configured per branch via regex
        [
          groovy: { logger "YO I RAN A CLOSURE FROM A CUSTOM STAGE!" }  // ex of running a groovy closure
        ], 
        [
          shell: 'customStageScript.sh'  // ex running a shell script
        ]
      ]
    ]
  ]
]
```

## Coverity
Coverity functionality requires the Coverity ~2.0.0 Plugin to be installed on the host Jenkins https://synopsys.atlassian.net/wiki/spaces/INTDOCS/pages/623018/Synopsys+Coverity+for+Jenkins#SynopsysCoverityforJenkins-Version2.0.0

## Custom Stages
The `stages` property of the BanzaiCfg allows you to override the order of existing Banzai Stages as well as provide custom Stages of your own. 

## BanzaiUserData
BanzaiUserData serves 2 purposes
1. Pass variables from a user-provided script in 1 stage to a user-provided script in another
2. Supply values to a Banzai-provided Stage that aren't known until a user-provided script runs

In order to persist BanzaiUserData you simply write a `BanzaiUserData.[yaml/json]` file to the root of the project workspace during the execution of a user-provided script. The file will be ingested by Banzai and deleted so you do not have to be concerned with any sort of file collision if you write more UserData in a subsequent stage. BanzaiUserData will be passed as the 1st argument to all user-provided scripts in json format. While you may store arbitraty data in the BanzaiUserData object, there are some fields which are read by Banzai-provided stages. The following fields are reserved

```
{
    "coverity": {
        "buildCmd": "mvn clean install"
    },
    "gitOps": {
      "versions": {
          "test-maven" : {
              "version": "1.0.0",
              "meta": {
                  "some": "example meta"
              }
          }
      }
    }
}
```

