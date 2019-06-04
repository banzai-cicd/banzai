package com.ge.nola;

class BanzaiBaseCfg {
    def clone(target) {
        def (sProps, tProps) = [this, target]*.properties*.keySet()
        def commonProps = sProps.intersect(tProps) - ['class', 'metaClass']
        commonProps.each { target[it] = this[it] }
        return target
    }
}