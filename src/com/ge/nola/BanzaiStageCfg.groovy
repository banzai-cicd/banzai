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

    def execute(BanzaiCfg cfg) {
        if (this.isBanzaiStage()) {
            List<String> parts = this.name.tokenize(':')
            String stageName = parts.removeAt(0)
            def args = [cfg] + parts
            // execute stage
            this."${stageName}Stage"(*args)
        }
    }
}