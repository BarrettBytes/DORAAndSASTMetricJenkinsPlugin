package io.jenkins.plugins.sample;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.security.ACL;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.CloneOption;
import org.jenkinsci.plugins.gitclient.GitClient;
import hudson.plugins.git.BranchSpec;
import java.util.Collections;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import hudson.model.Result;
import java.io.InputStream;

import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import hudson.model.Job;

import java.nio.file.Files;
import java.util.Queue;

import javax.servlet.ServletException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import groovy.lang.GroovyShell;
import groovy.lang.Binding;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;


public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private boolean useFrench;
    private String gitCredentialsId;
    private String dockerCredentialsId;
    private String SUDO_password;
    private String repositoryUrl;
    private boolean enableGitCheckout;
    @DataBoundConstructor
    public HelloWorldBuilder(String name) {
        this.name = name;
        this.gitCredentialsId = gitCredentialsId;
        this.dockerCredentialsId = dockerCredentialsId;
        this.SUDO_password = SUDO_password;
    }

    @DataBoundSetter
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    @DataBoundSetter
    public void setEnableGitCheckout(boolean enableGitCheckout) {
        this.enableGitCheckout = enableGitCheckout;
    }

    public String getName() {
        return name;
    }

    public boolean isUseFrench() {
        return useFrench;
    }

    @DataBoundSetter
    public void setUseFrench(boolean useFrench) {
        this.useFrench = useFrench;
    }

    
    private void checkoutRepository(FilePath workspace, TaskListener listener, Run<?, ?> run, Launcher launcher, String repositoryUrl, StandardUsernamePasswordCredentials gitCredentials)
    throws InterruptedException, IOException {
    try {
        listener.getLogger().println("Checking out repository: " + repositoryUrl);

        GitSCM scm = new GitSCM(
            Collections.singletonList(new UserRemoteConfig(repositoryUrl, null, null, gitCredentials.getId())),
            Collections.singletonList(new BranchSpec("*/main")), // Adjust the branch if necessary
            false, Collections.<SubmoduleConfig>emptyList(),
            null, null, Collections.<GitSCMExtension>singletonList(new CloneOption(true, true, null, null)));

        // Use the SCM object to check out the code
        scm.checkout(run, launcher, workspace, listener, null, null);

    } catch (Exception e) {
        listener.getLogger().println("Failed to checkout repository: " + e.getMessage());
        e.printStackTrace(listener.getLogger());
    }
}
    

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        try {

            StandardUsernamePasswordCredentials gitCredentials = null;
            if (gitCredentialsId != null && !gitCredentialsId.isEmpty()) {
                gitCredentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstance(),
                        ACL.SYSTEM,
                        Collections.emptyList()),
                    CredentialsMatchers.withId(gitCredentialsId));
            }
    
        
              Jenkins jenkins = Jenkins.get();
    if (jenkins == null) {
        listener.getLogger().println("Jenkins instance is null.");
        return;
    }

    if (enableGitCheckout && repositoryUrl != null && !repositoryUrl.isEmpty()) {
        checkoutRepository(workspace, listener, run, launcher, repositoryUrl, gitCredentials);

    }

  
  // Read the Jenkinsfile from the plugin resources
InputStream jenkinsfileStream = getClass().getClassLoader().getResourceAsStream("Jenkinsfile");
if (jenkinsfileStream == null) {
    listener.getLogger().println("Jenkinsfile resource not found in plugin.");
    return;
}

        // Create a FilePath object for the destination
        FilePath jenkinsfilePath = new FilePath(workspace, "Jenkinsfile");

        // Copy the Jenkinsfile to the workspace
        jenkinsfilePath.copyFrom(jenkinsfileStream);

        listener.getLogger().println("Jenkinsfile copied to workspace.");

    if (!jenkinsfilePath.exists()) {
        listener.getLogger().println("Jenkinsfile does not exist in the workspace.");
        return;
    }

    String jenkinsfileContent = jenkinsfilePath.readToString();

        WorkflowJob job = (WorkflowJob) jenkins.getItemByFullName("MyUniquePipelineJob");
        if (job == null) {
            // Job doesn't exist, create it
            job = new WorkflowJob(jenkins, "MyUniquePipelineJob");
            jenkins.add(job, "MyUniquePipelineJob");
        }
        // Set or update the job definition
        job.setDefinition(new CpsFlowDefinition(jenkinsfileContent, true));

        listener.getLogger().println("Jenkinsfile executing as a Pipeline job.");
        
        // Schedule the job
        job.scheduleBuild2(0);
        
        // Wait for the job to start
        WorkflowRun workflowRun = job.scheduleBuild2(0).waitForStart();

        // Wait for the job to complete
        while (workflowRun.getResult() == null) {
            Thread.sleep(1000); // Sleep for 1 second before checking again
        }

            // Get the console output of the completed job
            Run<?, ?> completedRun = ((Job<?, ?>) jenkins.getItemByFullName("MyUniquePipelineJob")).getLastBuild();
            if (completedRun != null && completedRun.getResult() != Result.NOT_BUILT) {
                String consoleOutput = completedRun.getLog();
                listener.getLogger().println("Console output of MyUniquePipelineJob:");
                listener.getLogger().println(consoleOutput);
            } else {
                listener.getLogger().println("Failed to retrieve the console output of MyUniquePipelineJob.");
            }

        } catch (Exception e) {
            listener.getLogger().println("Error executing Jenkinsfile: " + e.getMessage());
            e.printStackTrace(listener.getLogger());
        }

        // Your original code
        if (useFrench) {
            listener.getLogger().println("Bonjour, " + name + "!");
        } else {
            listener.getLogger().println("Hello, " + name + "!");
        }
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingName());
            if (value.length() < 4)
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_tooShort());
            if (!useFrench && value.matches(".*[éáàç].*")) {
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_reallyFrench());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }
    }
}
