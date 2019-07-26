library "BanzaiCICD@${BRANCH_NAME}"

banzai([
  appName: 'banzai',
  cleanWorkspace: [
    pre: true
  ],
  gitTokenId: 'git-token-user-pass',
  build: [
    /.*/ : [:]
  ],
  downstreamBuilds: [
    /PR-.*/: [
      [
        id: 'test-maven-build-master',
        job: '/banzai-cicd/test-maven-build/master',
        wait: true
      ]
    ]
  ]
])
