/*
 given the configuration for a specific stage
 that is branch-based. determine if the stage should run.
 returns false OR an object representing the config for the stage
*/
def call(stageConfig) {
    if ()
    if (stageConfig instanceof String) {
        return BRANCH_NAME ==~ stageConfig ? [:] : false
    } else if (stageConfig instanceof Map) {
        def key = stageConfig.keySet().find { BRANCH_NAME ==~ it }
        return key ? stageConfig[key] : false
    } else {
        return false
    }
}