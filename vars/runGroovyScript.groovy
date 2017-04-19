

def call(scriptFile, config) {
  def binding = new Binding()
  binding.setVariable('config', config)
  GroovyShell shell = new GroovyShell(binding);
  try {
    shell.evaluate(scriptFile);
  } catch (GroovyRuntimeException e) {
    throw e.getCause()
  }
}
