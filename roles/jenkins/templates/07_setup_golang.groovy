#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.model.Jenkins
import org.jenkinsci.plugins.golang.*
import java.util.logging.Logger

def logger = Logger.getLogger("")
def golang_version = "{{ golang_version }}"

def jenkins = Jenkins.getInstance()

Thread.start {
    sleep 10000

    // Golang
    logger.info('Configuring Golang.')
    def desc_GolangInst = jenkins.getDescriptor("org.jenkinsci.plugins.golang.GolangInstallation")

    def golangInstaller = new GolangInstaller(golang_version)
    def goInstallSourceProperty = new InstallSourceProperty([golangInstaller])
    def golang_inst = new GolangInstallation("GO_" + golang_version, "", [goInstallSourceProperty])

    // Only add Golang if it does not exist - do not overwrite existing config
    def golang_installations = desc_GolangInst.getInstallations()
    def golang_inst_exists = false
    golang_installations.each {
        installation = (GolangInstallation) it
        if (golang_inst.getName() == installation.getName()) {
            golang_inst_exists = true
            logger.info("Found existing golang installation: " + installation.getName())
        }
    }

    if (!golang_inst_exists) {
        golang_installations += golang_inst
        desc_GolangInst.setInstallations((GolangInstallation[]) golang_installations)
        desc_GolangInst.save()
    }

    // Save the state
    jenkins.save()
}