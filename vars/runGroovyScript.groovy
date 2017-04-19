

def call(scriptFile, config) {
  def binding = new Binding();
  binding.setVariable('sh', sh);
  binding.setVariable('config', config)
  GroovyShell shell = new GroovyShell(binding);
  shell.evaluate(scriptFile);
}
