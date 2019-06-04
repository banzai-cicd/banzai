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
            /*
                jenkins doesn't support the friggin spread operator so I can't do
                this."${stageName}Stage"(*args)
                which would be a nice one-liner for supporting stages w/ variable args
                ugggghhhhhhhhhhh
            */
            if (stageName == 'scans') {
                "${stageName}Stage"(args[0], args[1])
            } else {
                "${stageName}Stage"(args[0])
            }
        }
    }
}