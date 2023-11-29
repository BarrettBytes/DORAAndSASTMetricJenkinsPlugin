package io.jenkins.plugins.sample;

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

// ...

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private boolean useFrench;

    @DataBoundConstructor
    public HelloWorldBuilder(String name) {
        this.name = name;
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

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        try {
              Jenkins jenkins = Jenkins.get();
    if (jenkins == null) {
        listener.getLogger().println("Jenkins instance is null.");
        return;
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
