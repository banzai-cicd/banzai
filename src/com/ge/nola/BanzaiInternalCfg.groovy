package com.ge.nola;

class BanzaiInternalGitOpsCfg {
    String TARGET_ENV
    String TARGET_STACK
    Map<String, String> SERVICE_VERSIONS_TO_UPDATE
}

class BanzaiInternalCfg {
    BanzaiInternalGitOpsCfg gitOps
}