package com.ge.nola.banzai.cfg;

class BanzaiBaseCfg {
    def clone(target) {
        def (sProps, tProps) = [this, target].collect { it.properties.keySet() }
        def commonProps = sProps.intersect(tProps) - ['class', 'metaClass']
        commonProps.each { target[it] = this[it] }
        return target
    }

     Map asMap() {
        this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
            [ (it.name):this."$it.name" ]
        }
    }
}