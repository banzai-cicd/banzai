package com.github.banzaicicd.cfg;

class BanzaiProxyCfg {
    String envVar
    String host
    String port

    String toString() {
        "${this.host}:${this.port}"
    }
}