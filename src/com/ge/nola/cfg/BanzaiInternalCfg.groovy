package com.ge.nola.cfg;

class BanzaiInternalGitOpsCfg {
    String TARGET_ENV
    String TARGET_STACK
    Map<String, String> SERVICE_VERSIONS_TO_UPDATE
    Boolean DEPLOY = false
    List<String> DEPLOY_ARGS
    String DEPLOYMENT_ID
    Boolean PIPELINE_FAILURE_NOTIF_SENT = false
}

class BanzaiInternalCfg {
    BanzaiInternalGitOpsCfg gitOps = new BanzaiInternalGitOpsCfg()
}