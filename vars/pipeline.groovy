
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  node {
    currentBuild.result = 'SUCCESS'
    echo "My branch is: ${BRANCH_NAME}"

    // checkout the branch that triggered the build
    checkout scm

    // run SAST
    if (config.sast) {
      try {
        sast(config)
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        return
      }
    }

    // run build
    if (config.build) {
      try {
        build(config)
      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        return
      }
    }

    // if branch == tag/* or develop buld package image and push
    if (config.deploy) {
      try {

      } catch (err) {
        echo "Caught: ${err}"
        currentBuild.result = 'FAILURE'
        return
      }
    }

  } // node

}
