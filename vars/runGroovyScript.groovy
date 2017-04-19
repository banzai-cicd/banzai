import groovy.lang.GroovyClassLoader

def call(scriptFile, config) {
  try {
    GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader())
    Class groovyClass = loader.parseClass(scriptFile)
		def buildScript = groovyClass.newInstance()
    buildScript.invokeMethod("buildIt", this.&sh)
  } catch (GroovyRuntimeException e) {
    throw e.getCause()
  }
}
