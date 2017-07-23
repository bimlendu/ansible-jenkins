#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.model.Jenkins
import jenkins.security.s2m.AdminWhitelistRule
import java.util.logging.Logger

def logger = Logger.getLogger("")
def jenkins = Jenkins.getInstance()

// https://wiki.jenkins.io/display/JENKINS/Slave+To+Master+Access+Control
logger.info('Enabling slave-to-master access control.')
Jenkins.instance.getInjector().getInstance(AdminWhitelistRule.class)
.setMasterKillSwitch(false)