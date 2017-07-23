#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.model.Jenkins
import org.jenkinsci.plugins.scriptsecurity.scripts.*
import java.util.logging.Logger

def logger = Logger.getLogger("")
def jenkins = Jenkins.getInstance()

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
