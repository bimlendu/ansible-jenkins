#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import jenkins.security.s2m.AdminWhitelistRule
import java.util.logging.Logger

def logger = Logger.getLogger("")
def installed = false
def initialized = false
def pluginParameter = "{{ jenkins_plugins }}"
def plugins = pluginParameter.split()
logger.info('' + plugins)
def jenkins = Jenkins.getInstance()
def pm = jenkins.getPluginManager()
def uc = jenkins.getUpdateCenter()

logger.info('Installing plugins.')
plugins.each {
  logger.info('Checking ' + it)
  if (!pm.getPlugin(it)) {
    logger.info('Looking UpdateCenter for ' + it)
    if (!initialized) {
      uc.updateAllSites()
      initialized = true
    }
    def plugin = uc.getPlugin(it)
    if (plugin) {
      logger.info('Installing ' + it)
      def installFuture = plugin.deploy()
      while(!installFuture.isDone()) {
        logger.info('Waiting for plugin install: ' + it)
        sleep(3000)
      }
      installed = true
    }
  }
}
if (installed) {
  logger.info('Plugins installed, initializing a restart!')
  jenkins.save()
  jenkins.restart()
}

logger.info('Creating initial user {{ jenkins_admin_username }}.')

def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount('{{ jenkins_admin_username }}','{{ jenkins_admin_password }}')
jenkins.setSecurityRealm(hudsonRealm)

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
jenkins.setAuthorizationStrategy(strategy)
jenkins.save()

logger.info('Enabling slave-to-master access control.')
// https://wiki.jenkins.io/display/JENKINS/Slave+To+Master+Access+Control
Jenkins.instance.getInjector().getInstance(AdminWhitelistRule.class)
.setMasterKillSwitch(false)

logger.info('Pre-approving scripts as required.')
import org.jenkinsci.plugins.scriptsecurity.scripts.*
ScriptApproval sa = ScriptApproval.get();

signatures = [
  "staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getText java.net.URL",
  "method groovy.json.JsonSlurperClassic parseText java.lang.String", 
  "method java.lang.AutoCloseable close",
  "new groovy.json.JsonSlurperClassic",
  "staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getAt java.lang.Object java.lang.String"
]

signatures.each {signature ->
  ScriptApproval.PendingSignature s = new ScriptApproval.PendingSignature(signature, false, ApprovalContext.create())
  sa.getPendingSignatures().add(s)
  sa.approveSignature(s.signature)
  logger.info('Approved signature: ' + signature)
}

logger.info('Initial setup done.')