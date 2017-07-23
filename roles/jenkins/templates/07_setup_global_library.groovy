#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.model.Jenkins
import java.util.logging.Logger
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import jenkins.plugins.git.GitSCMSource;

def logger = Logger.getLogger("")
def jenkins = Jenkins.getInstance()

def shared_library = "{{ shared_library }}"

def globalLibsDesc = jenkins.getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")
SCMSourceRetriever retriever = new SCMSourceRetriever(new GitSCMSource(
	"",
    shared_library,
    "",
    "*",
    "",
    false))
LibraryConfiguration pipeline = new LibraryConfiguration("mySharedLibs", retriever)
pipeline.setDefaultVersion('master')
pipeline.setImplicit(true)
globalLibsDesc.get().setLibraries([pipeline])

jenkins.save()