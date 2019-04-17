#!/usr/bin/env groovy

import java.util.regex.Pattern

def call(config) {
    stage ('Publish') {
      runScript(config, "publishScriptFile", "publishScript")
    }
}
