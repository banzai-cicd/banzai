library "BanzaiCICD@${BRANCH_NAME}"

banzai([
  appName: 'banzai',
  cleanWorkspace: [
    pre: true
  ],
  gitTokenId: 'sweeney-git-token',
  flowdock: [
    banzaiFlow: [
      credId: 'banzai-flowtoken',
      author: [
        name: 'Banzai',
        avatarUrl: 'https://github.com/avatars/u/55576?s=400&u=700c7e70356d1f5a679908c1d7c7e5bf8e2beab6',
        email: 'banzai@email.com'
      ]
    ] 
  ],
  notifications: [
    flowdock: [
      /.*/: [
        'banzaiFlow': ['.*']
      ]
    ]
  ],
  downstreamBuilds: [
    /PR-.*/: [
      [
        id: 'test-maven-build-develop',
        job: '/Banzai/TestMavenBuild/develop',
        wait: true
      ]
    ]
  ]
])
