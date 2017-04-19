import groovy.lang.GroovyClassLoader

def call(scriptFile, config) {
  try {
    GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader())
    Class groovyClass = loader.parseClass(scriptFile)
		BuildScript buildScript = (BuildScript) groovyClass.newInstance()
    buildScript.invokeMethod("run", this.&sh)
  } catch (GroovyRuntimeException e) {
    throw e.getCause()
  }
}
