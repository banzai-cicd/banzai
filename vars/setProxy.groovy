#!/usr/bin/env groovy
// TODO: hate this..refactor
def call(config) {
    if (config.httpsProxy?.envVar) {
        logger "Setting HTTPS Proxy from environment variable ${config.httpsProxy.envVar}"
        def hostAndPort = env[config.httpsProxy.envVar].tokenize(":")
        config.httpsProxy = [
            protocol: 'https',
            host: hostAndPort[0],
            port: hostAndPort[1]
        ]
    }
    if (config.httpProxy?.envVar) {
        logger "Setting HTTP Proxy from environment variable ${config.httpProxy.envVar}"
        def hostAndPort = env[config.httpProxy.envVar].tokenize(":")
        config.httpProxy = [
            protocol: 'http',
            host: hostAndPort[0],
            port: hostAndPort[1]
        ]
    }

    config.noProxy = config.noProxy ?: env.no_proxy ?: env.NO_PROXY

    if (config.httpsProxy) {
        config.httpsProxy.protocol = 'https'
        logger "HTTPS PROXY set to ${config.httpsProxy.getUrl()}"
    }

    if (config.httpProxy) {
        config.httpProxy.protocol = 'http'
        logger "HTTP PROXY set to ${config.httpProxy.getUrl()}"
    }

    if (config.httpsProxy || config.httpProxy) {
        logger "NO PROXY set to ${config.noProxy}"
    }
}