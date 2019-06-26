package com.ge.nola.banzai.cfg;

class BanzaiGitOpsCfg {
    Map<String, String> autoDeploy
    String skipVersionUpdating // branch regex
    Map<String, BanzaiGitOpsEnvCfg> envs
    Map<String, BanzaiGitOpsInputCfg> inputCfg
}