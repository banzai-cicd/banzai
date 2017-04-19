import org.codehaus.groovy.control.CompilerConfiguration

def call(scriptFile, config) {
  @NonCPS
  def compilerConfiguration = new CompilerConfiguration()
  compilerConfiguration.scriptBaseClass = DelegatingScript.class.name
  // Configure the GroovyShell and pass the compiler configuration.
  def binding = new Binding()
  binding.setVariable('config', config)
  def shell = new GroovyShell(this.class.classLoader, binding, compilerConfiguration)

  // get an instance of our current class as it should have all the methods we want to call from the script
  def instanceOfThis = this.getClass().newInstance()

  // parse the scriptFile
  def script = shell.parse(scriptFile)
  // set the delegate of our script file to an instance of the class we're currently in.
  script.setDelegate(instanceOfThis)

  // def binding = new Binding()
  // binding.setVariable('config', config)
  // GroovyShell shell = new GroovyShell(binding);
  try {
    script.run();
  } catch (GroovyRuntimeException e) {
    throw e.getCause()
  }
}
