package com.ge.nola;

// allows users to define their own stage
class BanzaiStageCfg {
    String name
    List<BanzaiStepCfg> steps

    private List<String> banzaiStageLabels = [
        'build', 
        'deploy', 
        'publish',
        'integrationTests', 
        'scans:vulnerability',
        'scans:quality'
    ]

    List<String> getBanzaiStageLabels() {
        return banzaiStageLabels
    }

    Boolean isBanzaiStage() {
        return this.banzaiStageLabels.contains(this.name)
    }
}