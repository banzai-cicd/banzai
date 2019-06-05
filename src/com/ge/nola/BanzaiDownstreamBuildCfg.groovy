package com.ge.nola;

class BanzaiDownstreamBuildCfg extends BanzaiBaseCfg {
    String id
    String job
    Boolean optional = false
    Boolean parallel = false
    Boolean propagate = false
    List<Object> parameters
}