#!/usr/bin/env groovy
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiQualityCfg

def call(BanzaiCfg cfg, BanzaiQualityCfg qualityCfg) {
    String toolName = cfg.tools?.sonarQube ?: 'SonarQube'
    withSonarQubeEnv(toolName) {
        def sonarqubeScannerHome = tool name: toolName, type: 'hudson.plugins.sonar.SonarRunnerInstallation';
        sh "${sonarqubeScannerHome}/bin/sonar-scanner -e -X -Dsonar.projectKey=${qualityCfg.projectKey} -Dsonar.sources=.";
    }
}