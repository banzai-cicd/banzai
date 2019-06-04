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
    Map<String, BanzaiUserStepCfg> build
    Map<String, BanzaiUserStepCfg> publish
    Map<String, BanzaiUserStepCfg> deploy
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
}