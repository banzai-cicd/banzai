package com.ge.nola;

class BanzaiGitOpsCfg {
    Map<String, String> autoDeploy
    Map<String, BanzaiGitOpsEnvCfg> envs

    class BanzaiGitOpsEnvCfg {
        List<String> approvers
        List<String> watchers
    }
}