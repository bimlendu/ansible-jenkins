#!groovy
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import hudson.util.Secret

def msg_provider_auth_id = "{{ msg_provider_auth_id }}"
def msg_provider_auth_token = "{{ msg_provider_auth_token }}"
def msg_provider_src_phone = "{{ msg_provider_src_phone }}"
def msg_provider_dest_phone = "{{ msg_provider_dest_phone }}"
def statuspage_api_key = "{{ statuspage_api_key }}"

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