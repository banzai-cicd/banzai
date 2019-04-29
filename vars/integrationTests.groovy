#!/usr/bin/env groovy

def call(config) {

  if (config.integrationTestsBranches && !(BRANCH_NAME ==~ config.integrationTestsBranches)) {
    logger "${BRANCH_NAME} does not match the integrationTestsBranches pattern. Skipping IT"
    return
  }

  runScript(config, "integrationTestsScriptFile", "integrationTestsScript", [BRANCH_NAME])
}
