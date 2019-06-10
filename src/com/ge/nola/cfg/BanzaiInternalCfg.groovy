package com.ge.nola.cfg;

class BanzaiInternalGitOpsCfg {
    String TARGET_ENV
    String TARGET_STACK
    Map<String, String> SERVICE_VERSIONS_TO_UPDATE
    Boolean DEPLOY = false
    List<String> DEPLOY_ARGS
}

class BanzaiInternalCfg {
    BanzaiInternalGitOpsCfg gitOps = new BanzaiInternalGitOpsCfg()
}