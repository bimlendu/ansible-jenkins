import hudson.model.*
import jenkins.model.*
import hudson.plugins.sonar.*
import hudson.plugins.sonar.model.TriggersConfig
import hudson.plugins.sonar.utils.SQServerVersions
import hudson.tools.*
import java.util.logging.Logger

def logger = Logger.getLogger("")

// Variables
def sonar_server_url = "{{ sonar_server_url }}"
def sonar_account_login = ""
def sonar_account_password = ""
def sonar_db_url = ""
def sonar_db_login = ""
def sonar_db_password = ""
def sonar_plugin_version = ""
def sonar_additional_props = ""

def sonar_runner_version = "{{ sonar_runner_version }}"

// Constants
def instance = Jenkins.getInstance()

Thread.start {
    sleep 10000

    // Sonar
    // Source: http://pghalliday.com/jenkins/groovy/sonar/chef/configuration/management/2014/09/21/some-useful-jenkins-groovy-scripts.html
    logger.info('Configuring SonarQube.')
    def SonarGlobalConfiguration sonar_conf = instance.getDescriptor(SonarGlobalConfiguration.class)

    def sonar_inst = new SonarInstallation(
        "Sonar", // Name
        sonar_server_url,
        SQServerVersions.SQ_5_3_OR_HIGHER, // Major version upgrade of server would require to change it
        "", // Token
        sonar_db_url,
        sonar_db_login,
        sonar_db_password,
        sonar_plugin_version,
        sonar_additional_props,
        new TriggersConfig(),
        sonar_account_login,
        sonar_account_password,
        "" // Additional Analysis Properties
    )

    // Only add Sonar if it does not exist - do not overwrite existing config
    def sonar_installations = sonar_conf.getInstallations()
    def sonar_inst_exists = false
    sonar_installations.each {
        installation = (SonarInstallation) it
        if (sonar_inst.getName() == installation.getName()) {
            sonar_inst_exists = true
            logger.info('Found existing installation: ' + installation.getName())
        }
    }

    if (!sonar_inst_exists) {
        sonar_installations += sonar_inst
        sonar_conf.setInstallations((SonarInstallation[]) sonar_installations)
        sonar_conf.save()
    }

    // Sonar Runner
    // Source: http://pghalliday.com/jenkins/groovy/sonar/chef/configuration/management/2014/09/21/some-useful-jenkins-groovy-scripts.html
    logger.info('Configuring SonarQubeScanner')
    def desc_SonarRunnerInst = instance.getDescriptor("hudson.plugins.sonar.SonarRunnerInstallation")

    def sonarRunnerInstaller = new SonarRunnerInstaller(sonar_runner_version)
    def installSourceProperty = new InstallSourceProperty([sonarRunnerInstaller])
    def sonarRunner_inst = new SonarRunnerInstallation("SonarQubeScanner ", "", [installSourceProperty])

    // Only add our Sonar Runner if it does not exist - do not overwrite existing config
    def sonar_runner_installations = desc_SonarRunnerInst.getInstallations()
    def sonar_runner_inst_exists = false
    sonar_runner_installations.each {
        installation = (SonarRunnerInstallation) it
        if (sonarRunner_inst.getName() == installation.getName()) {
            sonar_runner_inst_exists = true
            logger.info('Found existing installation: ' + installation.getName())
        }
    }

    if (!sonar_runner_inst_exists) {
        sonar_runner_installations += sonarRunner_inst
        desc_SonarRunnerInst.setInstallations((SonarRunnerInstallation[]) sonar_runner_installations)
        desc_SonarRunnerInst.save()
    }

    // Save the state
    instance.save()
}