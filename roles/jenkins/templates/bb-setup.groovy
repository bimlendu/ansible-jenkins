#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.model.Jenkins
import org.jenkinsci.plugins.scriptsecurity.scripts.*
import jenkins.security.s2m.AdminWhitelistRule
import org.jenkinsci.plugins.golang.*
import hudson.plugins.sonar.*
import hudson.plugins.sonar.model.TriggersConfig
import hudson.plugins.sonar.utils.SQServerVersions
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import hudson.util.Secret
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.GitSCM
import java.util.logging.Logger

def logger = Logger.getLogger("")
def jenkins = Jenkins.getInstance()

def sonarqube_server_url = "{{ sonarqube_server_url }}"
def sonar_scanner_version = "{{ sonar_scanner_version }}"

def caddy_pipeline_job = "{{ caddy_pipeline_job }}"
def caddy_pipeline_repo = "{{ caddy_pipeline_repo }}"
def caddy_pipeline_file = "{{ caddy_pipeline_file }}"

def msg_provider_auth_id = "{{ msg_provider_auth_id }}"
def msg_provider_auth_token = "{{ msg_provider_auth_token }}"
def msg_provider_src_phone = "{{ msg_provider_src_phone }}"
def msg_provider_dest_phone = "{{ msg_provider_dest_phone }}"
def statuspage_api_key = "{{ statuspage_api_key }}"

def golang_version = "{{ golang_version }}"

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
def goInstallSourceProperty = new InstallSourceProperty([golangInstaller])
def golang_inst = new GolangInstallation("GO_GO", "", [goInstallSourceProperty])
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
def sonarRunnerInstaller = new SonarRunnerInstaller(sonar_scanner_version)
def sonarInstallSourceProperty = new InstallSourceProperty([sonarRunnerInstaller])
def sonarRunner_inst = new SonarRunnerInstallation("SonarQubeScanner", "", [sonarInstallSourceProperty])
def sonar_runner_installations = desc_SonarRunnerInst.getInstallations()
sonar_runner_installations += sonarRunner_inst
desc_SonarRunnerInst.setInstallations((SonarRunnerInstallation[]) sonar_runner_installations)
desc_SonarRunnerInst.save()
jenkins.save()

// Add Credetials
logger.info('Adding required credentials.')
domain = Domain.global()
store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

msg_providerAuth = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  "msg_provider",
  "MSG Provdier Auth",
  msg_provider_auth_id,
  msg_provider_auth_token
)

statuspageAPIKey = new StringCredentialsImpl(
CredentialsScope.GLOBAL,
"statusPageAPIKey",
"statuspage.io API Key",
Secret.fromString(statuspage_api_key))

msg_providerSrc = new StringCredentialsImpl(
CredentialsScope.GLOBAL,
"srcPhone",
"MSG Provdier src phone number",
Secret.fromString(msg_provider_src_phone))

msg_providerDst = new StringCredentialsImpl(
CredentialsScope.GLOBAL,
"destPhone",
"MSG Provdier dst phone number",
Secret.fromString(msg_provider_dest_phone))

store.addCredentials(domain, msg_providerAuth)
store.addCredentials(domain, statuspageAPIKey)
store.addCredentials(domain, msg_providerSrc)
store.addCredentials(domain, msg_providerDst)

// Create job
logger.info('Creating first pipeline job.')
WorkflowJob job = Jenkins.instance.createProject(WorkflowJob, caddy_pipeline_job)
def definition = new CpsScmFlowDefinition(new GitSCM(caddy_pipeline_repo),caddy_pipeline_file)
definition.lightweight = true
job.definition = definition

logger.info('Initial setup done. Restarting once.')
jenkins.save()
jenkins.restart()