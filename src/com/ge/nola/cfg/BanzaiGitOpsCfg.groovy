package com.ge.nola.cfg;

class BanzaiGitOpsCfg {
    Map<String, String> autoDeploy
    String skipVersionUpdating // branch regex
    Map<String, BanzaiGitOpsEnvCfg> envs
}