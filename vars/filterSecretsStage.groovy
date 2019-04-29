#!/usr/bin/env groovy

def call(config) {
  def stageName = 'Filter Secrets'

  if (config.filterSecrets) {
    // determine if branch matches and filterSecrets branches
    def secretConfigKey = config.filterSecrets.keySet().find { it ==~ BRANCH_NAME }
    if (!secretConfigKey) {
        logger "filterSecrets does not contain an entry that matches the branch: ${BRANCH_NAME}. Skipping ${stageName}"
        return
    }

    stage (stageName) {
      notify(config, stageName, 'Pending', 'PENDING')
      def secretConfig = config.filterSecrets[secretConfigKey]
      filterSecrets(secretConfig)
      notify(config, stageName, 'Successful', 'PENDING')
    }
  }
}
