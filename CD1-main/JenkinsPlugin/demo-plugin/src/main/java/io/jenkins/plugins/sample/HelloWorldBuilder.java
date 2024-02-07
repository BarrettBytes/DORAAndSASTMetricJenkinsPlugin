package io.jenkins.plugins.sample;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import org.kohsuke.stapler.interceptor.RequirePOST;


import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.Permission;
import hudson.model.Queue.Task;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.CloneOption;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.ListBoxModel;

import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.StepExecutionIterator;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.File;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

import java.util.Queue;

import javax.servlet.ServletException;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import hudson.util.ListBoxModel;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import hudson.plugins.git.SubmoduleConfig;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;


public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private boolean useFrench;
    private String gitCredentialsId;
    private String dockerCredentialsId;
    private String SUDO_password;
    private String repositoryUrl;
    private String credentialsId;
    private boolean enableGitCheckout;

    public ListBoxModel doFillGitCredentialsIdItems() {
        ListBoxModel result = new ListBoxModel();

        // Add all credentials to the dropdown
        for (StandardUsernamePasswordCredentials cred : CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class, Jenkins.getInstanceOrNull(), ACL.SYSTEM, Collections.emptyList())) {
            result.add(cred.getUsername(), cred.getId());
        }

        return result;
    }

    @RequirePOST
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath AbstractProject project) {
        StandardListBoxModel result = new StandardListBoxModel();
        result.includeEmptyValue();
        result.includeMatchingAs(
                ACL.SYSTEM,
                Jenkins.getInstanceOrNull(),
                StandardCredentials.class,
                Collections.emptyList(),
                CredentialsMatchers.always()
        );
        return result;
    }
    

    @DataBoundConstructor
public HelloWorldBuilder(String name, String gitCredentialsId, String dockerCredentialsId, String SUDO_password, String credentialsId) {
    this.name = name;
    this.gitCredentialsId = gitCredentialsId;
    this.dockerCredentialsId = dockerCredentialsId;
    this.SUDO_password = SUDO_password;
    this.credentialsId = credentialsId;
}


    public String getCredentialsId() {
        return this.credentialsId;
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

            // Delete existing pipeline job if it exists
        WorkflowJob existingJob = Jenkins.getInstance().getItem("MyPipelineJob", Jenkins.getInstance(), WorkflowJob.class);
        if (existingJob != null) {
            existingJob.delete();
        }

        // Create new pipeline job
        WorkflowJob job = Jenkins.getInstance().createProject(WorkflowJob.class, "MyPipelineJob");

        // Check if the job creation was successful
        if (job == null) {
            throw new IOException("Failed to create MyPipelineJob");
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

              // Execute a command with sudo using the sudo password provided
        if (SUDO_password != null && !SUDO_password.isEmpty()) {
            String commandWithSudo = "echo '" + SUDO_password + "' | sudo -S <your_command_here>";
            // Replace <your_command_here> with the command you want to execute with sudo
            // For example: "sudo -u someuser ls -l" or "sudo docker ps"
            int exitCode = launcher.launch().cmdAsSingleString(commandWithSudo).pwd(workspace).stdout(listener).join();
            if (exitCode != 0) {
                listener.getLogger().println("Failed to execute command with sudo.");
                // Handle failure gracefully or throw an exception if needed
            }
        } else {
            listener.getLogger().println("SUDO password not provided. Skipping sudo command execution.");
            // Handle the case where sudo password is not provided
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
