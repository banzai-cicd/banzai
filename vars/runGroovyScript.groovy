

def call(scriptFile, config) {
  def binding = new Binding()
  def myShell = sh
  binding.setProperty('sh', myShell);
  binding.setProperty('config', config)
  GroovyShell shell = new GroovyShell(binding);
  shell.evaluate(scriptFile);
}
