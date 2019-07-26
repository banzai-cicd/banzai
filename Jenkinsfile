library "BanzaiCICD@${BRANCH_NAME}"

banzai([
  appName: 'banzai',
  cleanWorkspace: [
    pre: true
  ],
  gitTokenId: 'git-token',
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
