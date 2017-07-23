#!groovy
import hudson.model.*
import jenkins.model.*
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.GitSCM
import java.util.logging.Logger

def logger = Logger.getLogger("")

def jenkins = Jenkins.getInstance()

def caddy_pipeline_job = "{{ caddy_pipeline_job }}"
def caddy_pipeline_repo = "{{ caddy_pipeline_repo }}"
def caddy_pipeline_file = "{{ caddy_pipeline_file }}"


Thread.start {
    sleep 10000

	logger.info('Creating pipeline job: ' + caddy_pipeline_job)

	def caddy_pipeline_exists = false
	Jenkins.instance.getAllItems().each { it ->
		if (it.fullName == caddy_pipeline_job) {
	    	caddy_pipeline_exists = true
	    	logger.info("Found existing job: " + caddy_pipeline_job)
	  	}
	}

	if (!caddy_pipeline_exists) {
	  	WorkflowJob job = Jenkins.instance.createProject(WorkflowJob, caddy_pipeline_job)
	  	def definition = new CpsScmFlowDefinition(new GitSCM(caddy_pipeline_repo),caddy_pipeline_file)
	  	definition.lightweight = true
	  	job.definition = definition
	}

	jenkins.save()

}
