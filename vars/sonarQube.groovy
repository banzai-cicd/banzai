#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiQualityCfg

def call(BanzaiQualityCfg cfg) {
    String toolName = cfg.tools?.sonarQube ?: 'SonarQube'
    withSonarQubeEnv(toolName) {
        def sonarqubeScannerHome = tool name: toolName, type: 'hudson.plugins.sonar.SonarRunnerInstallation';
        sh "${sonarqubeScannerHome}/bin/sonar-scanner -e -X -Dsonar.projectKey=${cfg.projectKey} -Dsonar.sources=.";
    }
}