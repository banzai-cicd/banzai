
def call(scriptFile, config) {
  def binding = new Binding();
  binding.setProperty('sh', sh);
  binding.setProperty('config', config)
  GroovyShell shell = new GroovyShell(binding);
  shell.evaluate(scriptFile);
}
