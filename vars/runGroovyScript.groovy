import groovy.lang.GroovyClassLoader

def call(scriptFile, config, ourSh) {
  try {
    GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader())
    Class groovyClass = loader.parseClass(scriptFile)
		def buildScript = groovyClass.newInstance()
    buildScript.invokeMethod("buildIt", ourSh)
  } catch (GroovyRuntimeException e) {
    throw e.getCause()
  }
}
