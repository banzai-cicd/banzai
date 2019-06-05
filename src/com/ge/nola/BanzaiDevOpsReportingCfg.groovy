package com.ge.nola;

class BanzaiDevOpsReportingCfg extends BanzaiBaseCfg {
    String branches
    String ci
    String uai
    String uaaCredId
    String uaaUrl
    String metricsUrl
    Map<String, BanzaiDevOpsReportingEnvCfg> environments
}