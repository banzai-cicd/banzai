package com.ge.nola;

// allows users to define their own stage
class BanzaiStageCfg {
    String name
    Map<String, List<BanzaiStepCfg>> steps

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

    public BanzaiStageCfg(LinkedHashMap props) {
        props.keySet().each { this[it] = props[it] }
        if (props.steps) {
            this.steps = [:]
            props.steps.keySet().each {
                this.steps[it] = props.steps[it].collect { stepMap -> new BanzaiStepCfg(stepMap) }
            }
        }
    }
}