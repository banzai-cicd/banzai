#!/usr/bin/env groovy
import com.ge.nola.cfg.BanzaiCfg
import com.ge.nola.cfg.BanzaiQualityCfg

def call(BanzaiCfg cfg, BanzaiQualityCfg qualityCfg) {
    String toolName = cfg.tools?.sonarQube ?: 'SonarQube'
    withSonarQubeEnv(toolName) {
        def sonarqubeScannerHome = tool name: toolName, type: 'hudson.plugins.sonar.SonarRunnerInstallation';
        sh "${sonarqubeScannerHome}/bin/sonar-scanner -e -X -Dsonar.projectKey=${qualityCfg.projectKey} -Dsonar.sources=.";
    }
}