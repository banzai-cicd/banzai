package com.ge.nola.banzai.cfg;

class BanzaiProxyCfg {
    String envVar
    String host
    String port

    String toString() {
        "${this.host}:${this.port}"
    }
}