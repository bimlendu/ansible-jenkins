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
def installed = false
def initialized = false
def pluginParameter = "{{ jenkins_plugins }}"
def plugins = pluginParameter.split()
def jenkins = Jenkins.getInstance()
def pm = jenkins.getPluginManager()
def uc = jenkins.getUpdateCenter()

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