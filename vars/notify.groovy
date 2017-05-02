
def call(config, stage, message, status, skipGit=false) {
  if (config.flowdock) {
    notifyFlowdock(config, stage, message, status)
  }

  if (!skipGit) {
    notifyGit(config, "${stage} ${message}", status)
  }

}
