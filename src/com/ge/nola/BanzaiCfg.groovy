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
    Boolean skipSCM
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
    Map<String, List<BanazaiDownstreamBuildCfg>> downstreamBuilds
    Map<String, BanazaiFilterSecretsCfg> filterSecrets
    BanzaiDevOpsReportingCfg powerDevOpsReporting
    BanzaiGitOpsTriggerCfg gitOpsTrigger
    BanzaiGitOpsCfg gitOps
    /* strictly for internal banzai usage */
    BanzaiInternalCfg internal = new BanzaiInternalCfg()
}