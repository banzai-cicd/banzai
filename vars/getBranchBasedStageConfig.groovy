/*
 given the configuration for a specific stage
 that is branch-based. determine if the stage should run.
 returns false OR an object representing the config for the stage
*/
def call(stageConfig) {
    logger "testing stageConfig ${stageConfig}"
    if (stageConfig instanceof String) {
        logger "String-based branch config"
        return BRANCH_NAME ==~ stageConfig ? [:] : false
    } else if (stageConfig instanceof Map) {
        logger "Map-based branch config"
        def key = stageConfig.keySet().find { BRANCH_NAME ==~ it }
        return key ? stageConfig[key] : false
    } else {
        return false
    }
}