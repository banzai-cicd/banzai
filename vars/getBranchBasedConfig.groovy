/*
 given the configuration for a specific stage
 that is branch-based. determine if the stage should run.
 returns null OR an object representing the config for the stage.
*/
def call(branchConfig, branchName = BRANCH_NAME) {
    if (branchConfig instanceof Map) {
        def key = branchConfig.keySet().find { branchName ==~ it }
        return key ? branchConfig[key] : null
    } else {
        return null
    }
}