package com.ge.nola;

class BanzaiDevOpsReportingEnvCfg {
    int key
    String name

    Map asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [ (it.name):this."$it.name" ]
        }
    }
}