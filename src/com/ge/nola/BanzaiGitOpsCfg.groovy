package com.ge.nola;

class BanzaiGitOpsCfg {
    Map<String, String> autoDeploy
    String skipVersionUpdating // branch regex
    Map<String, BanzaiGitOpsEnvCfg> envs
}