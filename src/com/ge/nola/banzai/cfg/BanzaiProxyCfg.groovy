package com.ge.nola.banzai.cfg;

class BanzaiProxyCfg {
    String envVar
    String host
    String port

    String getUrl() {
        "${this.host}:${this.port}"
    }
}