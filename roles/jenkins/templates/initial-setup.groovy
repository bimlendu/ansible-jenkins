#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.security.s2m.AdminWhitelistRule
import org.jenkinsci.plugins.golang.*
import hudson.plugins.sonar.*
import hudson.plugins.sonar.model.TriggersConfig
import hudson.plugins.sonar.utils.SQServerVersions
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.GitSCM
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import hudson.util.Secret

import java.util.logging.Logger

def logger = Logger.getLogger("")
def installed = false
def initialized = false
def pluginParameter = "{{ jenkins_plugins }}"
def plugins = pluginParameter.split()
def jenkins = Jenkins.getInstance()
def pm = jenkins.getPluginManager()
def uc = jenkins.getUpdateCenter()

def sonar_server_url = "{{ sonar_server_url }}"
def sonar_runner_version = "{{ sonar_runner_version }}"

def caddy_pipeline_job = "{{ caddy_pipeline_job }}"
def caddy_pipeline_repo = "{{ caddy_pipeline_repo }}"
def caddy_pipeline_file = "{{ caddy_pipeline_file }}"

def plivo_auth_id = "{{ plivo_auth_id }}"
def plivo_auth_token = "{{ plivo_auth_token }}"
def plivo_src_phone = "{{ plivo_src_phone }}"
def plivo_dest_phone = "{{ plivo_dest_phone }}"
def statuspage_api_key = "{{ statuspage_api_key }}"

// Install all plugins
logger.info('Installing plugins.')
logger.info('' + plugins)
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

// Admin user
logger.info('Creating initial user {{ jenkins_admin_username }}.')
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount('{{ jenkins_admin_username }}','{{ jenkins_admin_password }}')
jenkins.setSecurityRealm(hudsonRealm)
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
jenkins.setAuthorizationStrategy(strategy)
jenkins.save()

// https://wiki.jenkins.io/display/JENKINS/Slave+To+Master+Access+Control
logger.info('Enabling slave-to-master access control.')
Jenkins.instance.getInjector().getInstance(AdminWhitelistRule.class)
.setMasterKillSwitch(false)

// Approve scripts
logger.info('Pre-approving required signatures.')
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

// Configure Go.
logger.info('Configuring Golang')
def desc_GolangInst = jenkins.getDescriptor("org.jenkinsci.plugins.golang.GolangInstallation")
def golangInstaller = new GolangInstaller(golang_version)
def installSourceProperty = new InstallSourceProperty([golangInstaller])
def golang_inst = new GolangInstallation("GO_GO", "", [installSourceProperty])
def golang_installations = desc_GolangInst.getInstallations()
golang_installations += golang_inst
desc_GolangInst.setInstallations((GolangInstallation[]) golang_installations)
desc_GolangInst.save()
jenkins.save()

// Setup SonarQube Server
logger.info('Configuring SonarQube.')
def SonarGlobalConfiguration sonar_conf = jenkins.getDescriptor(SonarGlobalConfiguration.class)
def sonar_inst = new SonarInstallation(
  "Sonar", // Name
  sonar_server_url,
  SQServerVersions.SQ_5_3_OR_HIGHER,
  "",
  "",
  "",
  "",
  "",
  "",
  new TriggersConfig(),
  "",
  "",
  ""
)

def sonar_installations = sonar_conf.getInstallations()
sonar_installations += sonar_inst
sonar_conf.setInstallations((SonarInstallation[]) sonar_installations)
sonar_conf.save()
jenkins.save()

// SonarScanner
logger.info('Configuring SonarScanner')
def desc_SonarRunnerInst = jenkins.getDescriptor("hudson.plugins.sonar.SonarRunnerInstallation")
def sonarRunnerInstaller = new SonarRunnerInstaller(sonar_runner_version)
def installSourceProperty = new InstallSourceProperty([sonarRunnerInstaller])
def sonarRunner_inst = new SonarRunnerInstallation("SonarQubeScanner", "", [installSourceProperty])
def sonar_runner_installations = desc_SonarRunnerInst.getInstallations()
sonar_runner_installations += sonarRunner_inst
desc_SonarRunnerInst.setInstallations((SonarRunnerInstallation[]) sonar_runner_installations)
desc_SonarRunnerInst.save()
jenkins.save()

// Add Credetials
logger.info('Adding required credentials.')
domain = Domain.global()
store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

plivoAuth = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  "plivo",
  "Plivo Auth",
  plivo_auth_id,
  plivo_auth_token
)

statuspageAPIKey = new StringCredentialsImpl(
CredentialsScope.GLOBAL,
"statusPageAPIKey",
"statuspage.io API Key",
Secret.fromString(statuspage_api_key))

plivoSrc = new StringCredentialsImpl(
CredentialsScope.GLOBAL,
"srcPhone",
"Plivo src phone number",
Secret.fromString(plivo_src_phone))

plivoDst = new StringCredentialsImpl(
CredentialsScope.GLOBAL,
"destPhone",
"Plivo dst phone number",
Secret.fromString(plivo_dest_phone))

store.addCredentials(domain, plivoAuth)
store.addCredentials(domain, statuspageAPIKey)
store.addCredentials(domain, plivoSrc)
store.addCredentials(domain, plivoDst)

// Create job
logger.info('Creating first pipeline job.')
WorkflowJob job = Jenkins.instance.createProject(WorkflowJob, caddy_pipeline_job)
def definition = new CpsScmFlowDefinition(new GitSCM(caddy_pipeline_repo),caddy_pipeline_file)
definition.lightweight = true
job.definition = definition

logger.info('Initial setup done. Restarting once.')
jenkins.save()
jenkins.restart()