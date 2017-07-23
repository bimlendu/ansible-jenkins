// Admin user
logger.info('Creating initial user {{ jenkins_admin_username }}.')
def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount('{{ jenkins_admin_username }}','{{ jenkins_admin_password }}')
jenkins.setSecurityRealm(hudsonRealm)
def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
jenkins.setAuthorizationStrategy(strategy)
jenkins.save()