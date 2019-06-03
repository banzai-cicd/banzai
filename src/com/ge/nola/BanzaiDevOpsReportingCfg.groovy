package com.ge.nola;

class BanzaiDevOpsReportingCfg {
    String branches
    String ci
    String uai
    String uaaCredId
    String uaaUrl
    String merticsUrl
    Map<String, BanzaiDevOpsReportingEnvCfg> environments

    class BanzaiDevOpsReportingEnvCfg {
        int key
        String name
    }
}