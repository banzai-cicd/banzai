![Banzai](https://i.imgur.com/QKdnoZ4.png)

Banzai
========

basic Jenkinsfile
```
banzai([
  appName: 'config-reviewer-server',
  sshCreds: ['dev-ssh'],                                                 // add jenkins ssh cred with id 'dev-ssh' to the pipeline context
  build: [ /.*/: [:] ],                                                  // build all branches (calls build.sh)
  publish: [ /tag\-(.*)|develop/: [script : 'scripts/my-publish.sh'] ],  // publish tags and develop branch changes. Execute scripts/my-publish.sh instead of the default publish.sh
  deploy: [ /tag\-(.*)|develop/: [:] ],                                  // deploy tags and develop branch changes (calls deploy.sh)
])
```


full list of options
```
@Library('Banzai@1.0.0') _ // only necessary if configured as a 'Global Pipeline Library'. IMPORTANT: the _ is required after @Library. 
banzai([
    appName: 'config-reviewer-server',          // **required** currently used only by SAST for determining the namespace to publish to.
    throttle: 'my-project',                     // comma-delimited list of throttle categories to apply. (https://github.com/jenkinsci/throttle-concurrent-builds-plugin)
    sshCreds: ['cred1', 'cred2'],                                    // a list of any ssh creds that may be needed in your pipeline
    skipSCM: true,                              // skip pulling down the branch that kicked off the build
    debug: false,                               // provides additional debug messaging
    gitTokenId: 'sweeney-git-token',            // a Jenkins credential id which points to a github token (required by downstreamBuilds)
    httpsProxy: [
      envVar: 'https_proxy',                    // optionally use an env variable from the host to set the proxy info. should be in "{host}:{port}" format.
      host: 'proxyhost',
      port: '80'
    ],
    preCleanWorkspace: true,                           // wipe workspace before each build
    postCleanWorkspace: true,                          // wipe workspace after each build    
    flowdock: [
      /.*/: [                                   // which branches should report notifications to Flowdock
        credId: 'flowdock-cred',
        notifyPRs: false,                       // *default = false* whether or not to notify Flowdock with pr status changes
        author: [
          name: 'Jenkins',
          avatarUrl: 'https://github.build.ge.com/avatars/u/23999?s=466'
          email: 'Service.MyJenkins@ge.com'
        ],
      ]
    ],
    build: [                                   // build configuration which matches all branches and calls the default build script. (build.sh)
      /.*/ : [:]
    ],
    publish: [                                 // publish configuration which matches all branches and specifies a custom publish script location
      /.*/ : [
        script: 'scripts/my-publish-script.sh'
      ]
    ],
    deploy: [                                 // deploy configuration which matches all branches and specifies a custom deploy script location
      /.*/ : [
        script: 'scripts/my-deploy-script.sh'
      ]
    ],
    jdk = 'jdk 10.0.1',                         // value must be the name given to a configured JDK in the Global Tools sections of Jenkins
    vulnerabilityAbortOnError,                  // globally set that all vulnerability scans should abort the pipeline if there is an Error
    qualityAbortOnError,                        // globally set that all quality scans should abort the pipeline if there is an Error
    vulnerabilityScans = [
      /develop|master/: [                      // run this collection of scans against develop
        [
          type: 'checkmarx',
          credId: 'ge-checkmarx',               // jenkins credential containing user/pass for checkmarx
          resultEmails: ['your_email@ge.com'],
          preset: '17',                         // defaults to '17'
          teamUUID: 'your-checkmarx-team-uuid',
          abortOnError: false                   // determines of this scan should cause the pipeline to abort if it results in an Error.
        ],
        [
          type: 'coverity',
          credId: 'coverity-auth-key-file',     // jenkins credId of type 'file' representing your users authentication key (exported from coverity server UI)
          toolId: 'coverity-2018.12',           // the id given to the Jenkins Global Tool installation for Coverity
          serverHost: 'coverity.power.ge.com',
          serverPort: '443',
          resultEmails: ['simon.townsend1@ge.com'],
          buildCmd: 'mvn -s ./settings.xml clean install -U', // the command coverity should wrap. alternatively, you can export BUILD_CMD in a previous pipeline step and it will be picked up.
          projectName: 'your-coverity-project-name',
          abortOnError: true
        ]
      ]
    ],
    qualityScans: [
      /develop|master/: [
        [
          type: 'sonar',
          serverUrl: 'https://my-sonar.ge.com'
          credId: 'sonar-auth-token-id'        // jenkins credential (of type secret) containing a sonar server auth token
        ]
      ]
    ],
    downstreamBuilds: [
      /develop/: [                             // 'develop' signifies that this collection of downstream build definition's will only run when the 'develop' branch is matched
        [
          id: 'my-job',
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch',
          optional: true                       // when true, the downstream build will only run if the Pull Request contains a label in the format 'build:<job-id>', ie) 'build:my-job'
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
          propagate: true                     // this would mark 'my-job' as failed if 'my-parallel-job-2' fails
        ],
        [
          id: 'my-serial-job',                // this build would run in serial AFTER the 2 parallel builds complete
          job: '/YOUR_PROJECT_FOLDER/Build/your-project/branch'
        ]
      ]
    ],
    filterSecrets: [
      /develop/: [
          file: 'settings.xml',                     // the filePath to filter relative to the jenkins WORKSPACE root
          label: 'myPass',                          // should appear in the file in the format ${banzai:myPass}
          secretId: 'my-jenkins-secret-id'          // the id of the secret on Jenkins
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
    gitOpsTrigger: [                      // should be present with-in a Service's .banzai when leveraging GitOps-style deployments
      jenkinsJob: '/Banzai/GitOps/master', // the path to the GitOps master job in jenkins
      branches: /develop|tag-*/,           // branches that should trigger a GitOps build
      stackId: 'dib'                       // the GitOps 'stack' that this service is a member of
    ],
    gitOps: [                             // should be present with-in a GitOps repo when leveraging GitOps-style deployments
      autoDeploy: [
        /develop/ : 'dev'              // in this example. when a Service's 'develop' branch triggers the GitOps job. It will automatically spawn a deployment to the 'dev' environment 
      ],
      envs: [
        'dev' : [:],                   // register an env with no additional configuration
        'qa' : [
            approvers: ['<jenkins-id>'], // approvers will be emailed for approval prior to this env moving forward with deployment
            watchers: ['<jenkins-id>']   // watchers will be emailed when an enviroment is deployed
        ]
      ]
    ],
    stages: [                             // override the the build-in Banzai Stage order and provide custom Stages
      [ name: 'build' ],                  // call a built-in Banzai Stage using a reserved name 'build|deploy|publish|integrationTests|scans:vulnerability|scans:quality'
      [
        name: 'My Arbitrary Stage',       // provide a custom Banzai Stage name
        steps: [
          /.*/: [                         // steps for a custom stage can be configured per branch via regex
            [
              closure: { logger "YO I RAN A CLOSURE FROM A CUSTOM STAGE!" }  // example of running a groovy closure
            ], 
            [
              script: 'customStageScript.sh'  // example running a shell script
            ]
          ]
        ]
      ]
    ],
  ]
])
```

### downstreamBuilds
*Note: downstreamBuilds requires that `gitTokenId` is defined*
The downstream build definition supports all of the properties documented by [Jenkins Pipeline Build Step](https://jenkins.io/doc/pipeline/steps/pipeline-build-step/) as well as 3 custom properties `id`, `optional` and `parallel`. `id` is used to map Github PR labels in the event that `optional` is set to `true`. When a build completes and the next build(s) have `parallel: true` then it will by default start those builds but will not wait for them to complete. There are 3 scenarios where the build will wait for parallel builds to complete:
1. the parallel build also has `wait: true`
2. the parallel build also has `propagate: true`
3. there is one or more non-parallel builds defined after the parallel build(s) that need to be executed once the parallel build(s) complete.

### GitOps (Recommended)
Banzai supports [GitOps-style](https://www.xenonstack.com/insights/what-is-gitops/) deployments. GitOps allows you to back your environments and handle their deployments from a single repository as well as decouple CI from CD (good for security). Once configured, your Service repositories will complete all CI Banzai Pipeline steps. If Banzai determines that a new version has been created or a deployment should take place it will trigger a new Banzai Pipeline that builds your GitOps repository. The GitOps repository is responsible for recording all versions of each Service and updating your 'Stacks' with the correct versions in each environment. [You can view an example GitOps repo here](https://github.build.ge.com/Banzai-CICD/GitOps). For our purposes, a 'Stack' is merely a collection of indvidual Services that should be deployed together.

There are 2 methods of deployment via Banzai GitOps.
1. automatic deployment
  - the .banzai of your GitOps repo can be configured to automatically trigger a deployment based on the git branch of the upstream Service that triggered the GitOps job
2. manual deployment
  1. A user manually runs 'Build With Parameters' on the GitOps master job manually from with-in Jenkins. 
  2. The user will be presented with a series of user-input stages.
  3. The user can choose between 2 different styles of deployment
    1. 'Select Specific Versions' - The user will provide the version for each Service that should be deployed
    2. 'Promote Stack From Another Environment' - The versions from one environment will be copied to another

#### GitOps Configuration
First, update the .banzai file in the repository of each Service that you would like to trigger a GitOps job
.banzai additions
```
# ensure that 'deploy' is removed then add:

gitOpsTrigger: [
  jenkinsJob: '/Banzai/GitOps/master', # the path to the GitOps master job in jenkins
  branches: /develop|tag-*/,           # branches that should trigger a GitOps build
  stackId: 'dib'                       # the GitOps 'stack' that this service is a member of
]
```

Next, create a GitOps repo. You can use the [GitOps-starter](https://github.build.ge.com/Banzai-CICD/GitOps-starter) to speed things up.  
**Your GitOps Repo Must Contain**  
- `envs` directory with sub-directories for each environment
- `services` directory (this is where the available versions of each service will be stored)
- `.banzai` file with a `gitOps` section
- `deployScript.sh` (will be called for each deployment)


### Coverity
Coverity functionality requires the Coverity ~2.0.0 Plugin to be installed on the host Jenkins https://synopsys.atlassian.net/wiki/spaces/INTDOCS/pages/623018/Synopsys+Coverity+for+Jenkins#SynopsysCoverityforJenkins-Version2.0.0

### Custom Stages
The `stages` property of the BanzaiCfg allows you to override the order of existing Banzai Stages as well as provide custom Stages of your own. Be aware, Stages and boilerplate that exist for supporting SCM, Power DevOps Reporting, Secrets Filtering, GitOps, proxy etc will still run. ie) when you leverage the `stages` property you will only be overriding the following Stages `vulnerabilityScans, qualityScans, build, publish, deploy, integrationTests`
