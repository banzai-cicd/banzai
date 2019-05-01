// jenkins-master-shared-library imports
import main.groovy.cicd.pipeline.settings.PipelineSettings;
import main.groovy.cicd.pipeline.plugins.CheckmarxPluginWrapper;
import main.groovy.cicd.pipeline.helpers.Utilities;

// jenkins API imports
import hudson.FilePath;
import jenkins.model.Jenkins;

def call(config)
{
    if (!config.powerDevOpsReporting) {
        logger "No config.powerDevOpsReporting, skipping powerDevOpsInitReportingSettings"
        return
    }
    logger "Initializing Power DevOps Reporting Settings"

    def sonarUrl
    def sonarCredId
    if (config.qualityScans) {
        logger "powerDevOpsInitReportingSettings qualityScans"
        def configKey = config.qualityScans.keySet().find { BRANCH_NAME ==~ it }
        if (configKey) {
            logger "powerDevOpsInitReportingSettings configKey ${configKey}"
            def sonarConfig = config.qualityScans[configKey].find { it.type == 'sonar' }
            if (sonarConfig) {
                logger "powerDevOpsInitReportingSettings sonarConfig"
                sonarUrl = sonarConfig.serverUrl
                sonarCredId = sonarConfig.credId
            }
        }
    }
    
    def reportingConfig = config.powerDevOpsReporting << [
        proxyHost: config.httpsProxyHost,
        proxyPort: config.httpsProxyPort,
        sonarUrl:  sonarUrl,
        sonarCredId: sonarCredId
    ]

    initializeApplicationMetadata(reportingConfig.uai, reportingConfig.ci);
    initializeCodeCheckoutSettings();
    initializeSonarQubeSettings(reportingConfig);
    initializeCheckmarxSettings();
    initializePipelineMetadata();
    initializeBuildSettings();
    //initializePublishSettings(reportingConfig);
    initializeDeploySettings(reportingConfig);
    initializeReportingSettings(reportingConfig);
    initializeProxySettings(reportingConfig);
}

private def initializeApplicationMetadata(uai, ci)
{   
    logger "initializeApplicationMetadata ${uai}, ${ci}"
    /*
    *   Application Metadata settings
    */
    // Pipeline config settings passed in as arguments in scripted pipeline from Jenkinsfile
    PipelineSettings.ApplicationMetadata.uai = uai;
    PipelineSettings.ApplicationMetadata.ci = ci;
}

String determineOrgName(url) {
    def finder = (url =~ /:([^:]*)\//)
    return finder.getAt(0).getAt(1)
}

String determineRepoName(url) {
    return scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
}

private def initializeCodeCheckoutSettings()
{
    logger "initializeCodeCheckoutSettings"
    /*
    *   Code Checkout settings
    */
    // determine base repo/branch git url info
    def url = scm.getUserRemoteConfigs()[0].getUrl()
    def orgName = determineOrgName(url)
    def repoName = determineRepoName(url)
    
    PipelineSettings.CodeCheckoutSettings.scm = 'github';
    PipelineSettings.CodeCheckoutSettings.baseUrl = 'https://github.build.ge.com';
    PipelineSettings.CodeCheckoutSettings.org = orgName;
    PipelineSettings.CodeCheckoutSettings.repo = repoName;
    PipelineSettings.CodeCheckoutSettings.branch = BRANCH_NAME;
    PipelineSettings.CodeCheckoutSettings.currentCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim(); //branchInfo.commit.sha;
}

private def initializeSonarQubeSettings(reportingConfig)
{
    logger "initializeSonarQubeSettings ${reportingConfig.sonarUrl}"
    /*
    *   SonarQube settings
    */
    // Set project key to use when calling SQ REST API
    PipelineSettings.SonarQubeSettings.projectKey = "${PipelineSettings.CodeCheckoutSettings.repo}:${PipelineSettings.CodeCheckoutSettings.branch}";
    PipelineSettings.SonarQubeSettings.sonarHostUrl = reportingConfig.sonarUrl;

    withCredentials([string(credentialsId: reportingConfig.sonarCredId, variable: 'SECRET')]) {
        PipelineSettings.SonarQubeSettings.sonarAuthToken = SECRET;
    }

    PipelineSettings.SonarQubeSettings.initializeQualityMetrics();
}

private def initializeCheckmarxSettings()
{
    logger "initializeCheckmarxSettings"
    PipelineSettings.CheckmarxSettings.initializeSastMetrics();
}

private def initializePipelineMetadata()
{
    /*
    *   Pipeline Metadata settings
    */
    // Pipeline introspection
    logger "initializePipelineMetadata"
    PipelineSettings.PipelineMetadata.url = JENKINS_URL;

    // Version of shared library - just set null for now, it's not important
    PipelineSettings.PipelineMetadata.version = null;
}

private def initializeBuildSettings()
{
    logger "initializeBuildSettings"
    // conditionally open file path on java.io.File(workspace) if node is master
    def nodeWorkspace = null;
    if(NODE_NAME == 'master' || env.NODE_NAME == null)
        nodeWorkspace = new FilePath(new File(pwd()));
    else
        nodeWorkspace = new FilePath(Jenkins.getInstance().getComputer(NODE_NAME).getChannel(), pwd());

    def maven = nodeWorkspace.child('pom.xml');
    def nodejs = nodeWorkspace.child('package.json');
    def settings = [:];

    // this implementation won't support multiple project types in a single repo
    if(maven.exists())
        settings = Utilities.getBuildSettingsFromPomXml(maven.readToString());

    if(nodejs.exists())
        settings = Utilities.getBuildSettingsFromPackageJson(nodejs.readToString());

    /*
    *   Build Settings
    */
    PipelineSettings.BuildSettings.artifactName = settings['artifactName'];
    PipelineSettings.BuildSettings.version = settings['version'];
}

private def initializePublishSettings()
{
    logger "initializePublishSettings"
    def nodeWorkspace = new FilePath(Jenkins.getInstance().getComputer(NODE_NAME).getChannel(), pwd());
    def publishScript = nodeWorkspace.child('publishScript.sh');
    def scriptContents = [];

    if (publishScript.exists())
        scriptContents = publishScript.readToString().tokenize('\n');

    def settings = Utilities.readScriptContents(scriptContents, ['dockerRepoUrl']);

    /*
    *   Publish settings
    */
    PipelineSettings.PublishSettings.repo = settings['dockerRepoUrl'];
    PipelineSettings.PublishSettings.hash = '';
    PipelineSettings.PublishSettings.password = '';
    PipelineSettings.PublishSettings.key = '';
}

private def initializeDeploySettings(reportingConfig)
{
    logger "initializeDeploySettings"
    def envKey = 0
    def branchKey = reportingConfig.environments.keySet().find { BRANCH_NAME ==~ it }
    if (branchKey) {
        def envConfig = reportingConfig.environments[branchKey]
        envKey = envConfig.key
    }

    PipelineSettings.DeploySettings.environment = envKey
}

private def initializeReportingSettings(reportingConfig)
{
    logger "initializeReportingSettings"
    /*
    *   Reporting settings
    */
    // Read URLs from parameter map and get UAA credentials from Jenkins
    PipelineSettings.ReportingSettings.metricsUrl = reportingConfig.metricsUrl;
    PipelineSettings.ReportingSettings.uaaUrl = reportingConfig.uaaUrl;

    withCredentials([usernamePassword(credentialsId: reportingConfig.uaaCredId, usernameVariable: 'UAA_CLIENT_ID', passwordVariable: 'UAA_CLIENT_SECRET')]) {
        PipelineSettings.ReportingSettings.uaaClientId = UAA_CLIENT_ID;
        PipelineSettings.ReportingSettings.uaaClientSecret = UAA_CLIENT_SECRET;
    }
}

private def initializeProxySettings(reportingConfig)
{
    logger "initializeProxySettings"
    /*
    *   Proxy settings
    */
    if (reportingConfig.proxyHost && reportingConfig.proxyPort) {
        PipelineSettings.ProxySettings.proxyHost = reportingConfig.proxyHost;
        PipelineSettings.ProxySettings.proxyPort = reportingConfig.proxyPort;
    }

}