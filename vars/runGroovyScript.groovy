

def call(scriptFile, config) {
  def binding = new Binding()
  binding.setProperty('config', config)
  GroovyShell shell = new GroovyShell(binding);
  try {
    shell.evaluate(scriptFile);
  } catch (GroovyRuntimeException e) {
    throw e.getCause()
  }
}
