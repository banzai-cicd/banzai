package com.ge.nola.cfg;

class BanzaiDownstreamBuildCfg extends BanzaiBaseCfg {
    String id
    String job
    Boolean optional = false
    Boolean parallel = false
    Boolean propagate = false
    Boolean wait = false
    int quietPeriod = 0
    List<Object> parameters
}