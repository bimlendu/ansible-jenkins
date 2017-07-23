#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.model.Jenkins
import java.util.logging.Logger

def logger = Logger.getLogger("")
def jenkins = Jenkins.getInstance()

// Admin user
logger.info('Creating initial user {{ jenkins_admin_username }}.')
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount('{{ jenkins_admin_username }}','{{ jenkins_admin_password }}')
jenkins.setSecurityRealm(hudsonRealm)
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
jenkins.setAuthorizationStrategy(strategy)
jenkins.save()