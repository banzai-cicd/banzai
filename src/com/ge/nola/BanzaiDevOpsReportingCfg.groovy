package com.ge.nola;

class BanzaiDevOpsReportingCfg {
    String branches
    String ci
    String uai
    String uaaCredId
    String uaaUrl
    String metricsUrl
    Map<String, BanzaiDevOpsReportingEnvCfg> environments

    Map asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [ (it.name):this."$it.name" ]
        }
    }
}