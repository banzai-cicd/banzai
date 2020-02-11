package com.github.banzaicicd.cfg;

class BanzaiCfg {
    String appName
    BanzaiToolsCfg tools
    List<String> throttle
    int timeout = 30
    List<String> sshCreds = []
    Boolean debug
    String gitTokenId
    String noProxy
    BanzaiProxyCfg proxy
    BanzaiCleanWorkspaceCfg cleanWorkspace
    Boolean skipSCM = false
    Map<String, BanzaiFlowdockCfg> flowdock
    Map<String, BanzaiEmailCfg> email
    BanzaiNotificationsCfg notifications
    Map<String, BanzaiStepCfg> build
    Map<String, BanzaiStepCfg> publish
    Map<String, BanzaiStepCfg> deploy
    Map<String, BanzaiIntegrationTestsCfg> integrationTests
    Boolean vulnerabilityAbortOnError
    Map<String, List<BanzaiVulnerabilityCfg>> vulnerabilityScans
    Boolean qualityAbortOnError
    Map<String, List<BanzaiQualityCfg>> qualityScans
    Map<String, List<BanzaiDownstreamBuildCfg>> downstreamBuilds
    Map<String, BanzaiFilterSecretsCfg> filterSecrets
    Map<String, BanzaiGitOpsTriggerCfg> gitOpsTrigger
    BanzaiGitOpsCfg gitOps
    /* strictly for internal banzai usage */
    BanzaiInternalCfg internal = new BanzaiInternalCfg()
    List<BanzaiStageCfg> stages
    Map<String, String> userData = [:] // a map of variables returned by bash scripts
    BanzaiHooksCfg hooks

    public BanzaiCfg(LinkedHashMap props) {
        /*
        whenever we have properties of type 'List' typed to an object we have to 
        manually populate them because Groovy's MapConstructor will
        leave them as LinkedHashMaps. Jenkins doesn't support
        @MapConstructor which would make this cleaner
        */
        props.keySet().each { this[it] = props[it] }
        if (props.stages) {
            this.stages = props.stages.collect { new BanzaiStageCfg(it) }
        }
        if (props.vulnerabilityScans) {
            props.vulnerabilityScans.keySet().each { branchKey ->
                this.vulnerabilityScans[branchKey] = 
                    props.vulnerabilityScans[branchKey].collect { new BanzaiVulnerabilityCfg(it) }
            }
        }
        if (props.qualityScans) {
            props.qualityScans.keySet().each { branchKey ->
                this.qualityScans[branchKey] = 
                    props.qualityScans[branchKey].collect { new BanzaiQualityCfg(it) }
            }
        }
    }
}