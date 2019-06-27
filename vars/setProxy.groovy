#!/usr/bin/env groovy
import jenkins.model.Jenkins;
import com.ge.nola.banzai.cfg.BanzaiCfg;

def call(BanazaiCfg cfg) {
    if (!cfg.proxy) {
        Jenkins j = Jenkins.getInstance();
        if (j.proxy == null) {
            // attempt setting proxy via env vars
            String envProxy = env.http_proxy ?: env.HTTP_PROXY
            if (envString != null) {
                String[] hostAndPort = envProxy.tokenize(":")
                cfg.proxy = [
                    host: hostAndPort[0],
                    port: hostAndPort[1]
                ]
            }
            cfg.noProxy = env.no_proxy ?: env.NO_PROXY
        } else {
            // default to proxy settings already set via Plugin Management -> Advanced
            cfg.proxy = [
                host: j.proxy.name,
                port: j.proxy.port
            ]
            cfg.noProxy = j.proxy.noProxyHost
        }
    }

    if (cfg.proxy) {
        logger "HTTP PROXY set to ${config.proxy.toString()}"
        logger "NO PROXY set to ${config.noProxy}"
    }
}