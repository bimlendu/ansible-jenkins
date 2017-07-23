#!groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.security.*
import hudson.tools.*
import jenkins.model.Jenkins
import java.util.logging.Logger
import org.jenkinsci.plugins.workflow.libs.*
import hudson.scm.SCM;
import hudson.plugins.git.*;

def logger = Logger.getLogger("")
def jenkins = Jenkins.getInstance()

def shared_library = "{{ shared_library }}"

def desc = jenkins.getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")

SCM scm = new GitSCM(shared_library)
SCMRetriever retriever = new SCMRetriever(scm)

def name = "mySharedLibs"    
LibraryConfiguration libconfig = new LibraryConfiguration(name, retriever)
desc.get().setLibraries([libconfig])

jenkins.save()