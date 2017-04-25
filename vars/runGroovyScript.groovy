#!/usr/bin/env groovy

import groovy.lang.GroovyClassLoader

/* currently not used, work in progress */
def call(scriptFile, config, ourSh) {
  try {
    @NonCPS
    GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader())
    Class groovyClass = loader.parseClass(scriptFile)
		def buildScript = groovyClass.newInstance()
    buildScript.invokeMethod("buildIt", ourSh)
  } catch (GroovyRuntimeException e) {
    throw e.getCause()
  }
}
