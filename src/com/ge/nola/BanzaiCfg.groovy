package com.ge.nola;

class BanzaiCfg {
    String appName
    String jdk
    String nodejs
    String throttle
    List<String> sshCreds
    Boolean debug
    String gitTokenId
    String noProxy
    BanzaiProxyCfg httpProxy
    BanzaiProxyCfg httpsProxy
    Boolean preCleanWorkspace
    Boolean postCleanWorkspace
    Boolean skipSCM = false
    String flowdockBranches
    String flowdockCredId
    Map<String, BanzaiFlowdockCfg> flowdock
    Map<String, BanzaiUserCfg> build
    Map<String, BanzaiUserCfg> publish
    Map<String, BanzaiUserCfg> deploy
    Map<String, BanzaiIntegrationTestsCfg> integrationTests
    Boolean vulnerabilityAbortOnError
    Map<String, List<BanzaiVulnerabilityCfg>> vulnerabilityScans
    Map<String, List<BanzaiQualityCfg>> qualityScans
    Map<String, List<BanzaiDownstreamBuildCfg>> downstreamBuilds
    Map<String, BanzaiFilterSecretsCfg> filterSecrets
    BanzaiDevOpsReportingCfg powerDevOpsReporting
    Map<String, BanzaiGitOpsTriggerCfg> gitOpsTrigger
    BanzaiGitOpsCfg gitOps
    /* strictly for internal banzai usage */
    BanzaiInternalCfg internal = new BanzaiInternalCfg()
    List<BanzaiStageCfg> stages

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
    }
}