package com.ge.nola.banzai.cfg;

class BanzaiProxyCfg {
    String envVar
    String protocol
    String host
    String port

    String getUrl() {
        "${this.protocol}://${this.host}:${this.port}"
    }
}