

// https://wiki.jenkins.io/display/JENKINS/Slave+To+Master+Access+Control
logger.info('Enabling slave-to-master access control.')
Jenkins.instance.getInjector().getInstance(AdminWhitelistRule.class)
.setMasterKillSwitch(false)