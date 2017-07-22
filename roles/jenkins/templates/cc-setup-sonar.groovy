import hudson.model.*
import jenkins.model.*
import hudson.plugins.sonar.*
import hudson.plugins.sonar.model.TriggersConfig
import hudson.plugins.sonar.utils.SQServerVersions
import hudson.tools.*
import java.util.logging.Logger

def logger = Logger.getLogger("")
def sonarqube_server_url = "{{ sonarqube_server_url }}"
def sonar_scanner_version = "{{ sonar_scanner_version }}"

def instance = Jenkins.getInstance()

Thread.start {
    sleep 10000
    
    logger.info('Configuring SonarQube.')
    def SonarGlobalConfiguration sonar_conf = instance.getDescriptor(SonarGlobalConfiguration.class)

    def sonar_inst = new SonarInstallation(
        "ADOP Sonar", // Name
        sonarqube_server_url,
        SQServerVersions.SQ_5_1_OR_LOWER, // Major version upgrade of server would require to change it
        "", // Token
        "",
        "",
        "",
        "",
        "",
        new TriggersConfig(),
        "",
        "",
        "" // Additional Analysis Properties
    )

    // Only add Sonar if it does not exist - do not overwrite existing config
    def sonar_installations = sonar_conf.getInstallations()
    def sonar_inst_exists = false
    sonar_installations.each {
        installation = (SonarInstallation) it
        if (sonar_inst.getName() == installation.getName()) {
            sonar_inst_exists = true
            logger.info("Found existing installation: " + installation.getName())
        }
    }

    if (!sonar_inst_exists) {
        sonar_installations += sonar_inst
        sonar_conf.setInstallations((SonarInstallation[]) sonar_installations)
        sonar_conf.save()
    }

    // Sonar Scanner
    logger.info('Configuring SonarScanner.')
    def desc_SonarRunnerInst = instance.getDescriptor("hudson.plugins.sonar.SonarRunnerInstallation")

    def sonarRunnerInstaller = new SonarRunnerInstaller(sonar_scanner_version)
    def installSourceProperty = new InstallSourceProperty([sonarRunnerInstaller])
    def sonarRunner_inst = new SonarRunnerInstallation("ADOP SonarRunner " + sonar_scanner_version, "", [installSourceProperty])

    // Only add SonarScanner if it does not exist - do not overwrite existing config
    def sonar_scanner_installations = desc_SonarRunnerInst.getInstallations()
    def sonar_scanner_inst_exists = false
    sonar_scanner_installations.each {
        installation = (SonarRunnerInstallation) it
        if (sonarRunner_inst.getName() == installation.getName()) {
            sonar_scanner_inst_exists = true
            logger.info("Found existing installation: " + installation.getName())
        }
    }

    if (!sonar_scanner_inst_exists) {
        sonar_scanner_installations += sonarRunner_inst
        desc_SonarRunnerInst.setInstallations((SonarRunnerInstallation[]) sonar_scanner_installations)
        desc_SonarRunnerInst.save()
    }

    // Save the state
    instance.save()
}