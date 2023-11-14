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

    // Function to wrap a string in square brackets
    private static String wrapInBrackets(String input) {
        return "[" + input + "]";
    }

    // Function to generate the regex pattern
    final String generateRegexPattern() {
        return "[^\\\\[]+";
    }

    // Function to generate the severity part
    final String generateSeverityPart() {
        return "\\\\[" + "$" + "{" + "severity" + "}" + " Severity\\\\]";
    }

    // Function to generate the full grep command
    final String generateGrepCommand() {
        return "grep -oE '✗ " + generateRegexPattern() + " " + generateSeverityPart()
                + "' PythonPlantsVsZombies/snyk_test_output.txt";
    }

    // Function to join strings with the pipe character
    private static String joinWithPipe(String... inputs) {
        return String.join("|", inputs);
    }

    public static String generatePushVulnerabilitiesCommand() {
        return "echo \"snyk_vulnerabilities_" + "$" + "{" + "globalCounter" + "}" + "{severity='" + "$" + "{" + "rating"
                + "}" +
                "', instance='" + "$" + "{" + "env.NODE_NAME" + "} " + "} " + "$" + "{" + "count" + "}"
                + "\" | curl --data-binary @- http://"
                + "$" + "{" + "JENKINS_HOST" + "}"
                + ":9091/metrics/job/snyk_scan\" ";
    }

    public static String generateScript() {

        String script = "totalVulnerabilities.each { rating, count ->\n" +
                "    echo \"Pushing ${rating} vulnerabilities count: ${count}\"\n" +
                "    sh '" + generatePushVulnerabilitiesCommand() + "\n" +
                "    globalCounter++\n" +
                "}";

        return script;
    }

    public static String atSymbol = "@";

    public static String generateScript2() {
        // needs fixing
        return "";
        /*
         * String script = "sh '''\n" +
         * "echo \"snyk_total_vulnerabilities_weighted{instance=\\\"${env.NODE_NAME}\\\"} ${totalWeightedScore}\" | "
         * +
         * "curl --data-binary @- http://${JENKINS_HOST}:9091/metrics/job/snyk_scan" +
         * "'''";
         * 
         * return script;
         */
    }

    String generateScript3() {
        String jenkinsHost = "YOUR_JENKINS_HOST"; // replace with actual value

        return "if (issues) {\n" +
                "    echo \"Pushing ${severity} vulnerabilities: ${issues}\"\n" +
                "    sh(script: '''\n" +
                "        echo \"snyk_issues_${issuesCounter}{severity=\"${severity}\", issues=\"${issues}\", instance=\"${env.NODE_NAME}\"} 1\" | curl --data-binary @- http://"
                + jenkinsHost + ":9091/metrics/job/snyk_scan\n" +
                "    ''')\n" +
                "    issuesCounter++\n" +
                "}";
    }

    String generateScript4() {
        // needs to be fixed
        /*
         * "                     sh '''\n" +
         * "                        echo \"snyk_scan_duration_seconds_${syncScanDurationCounter}{instance=\\\"${env.NODE_NAME}\\\"} ${env.SNYK_SCAN_DURATION}\" | curl --data-binary @- http://${JENKINS_HOST}:9091/metrics/job/snyk_scan"
         * +
         */
        return "";
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        String severityPattern = wrapInBrackets(joinWithPipe("Low", "Medium", "High", "Critical") + " Severity");
        String grepCommand = "grep -qE '" + severityPattern + "' PythonPlantsVsZombies/snyk_test_output.txt";

        String hardcodedScript = "pipeline {\n" +
                "    agent any\n" +
                "    environment {\n" +
                "        SNYK_TOKEN = '50b15e06-0453-4c5c-ad25-942a6808536e'\n" +
                "        JENKINS_HOST = new URL(env.JENKINS_URL).getHost()\n" +
                "    }\n" +
                "    stages {\n" +
                "        stage('Determine Agent') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    if (isUnix()) {\n" +
                "                        if (sh(script: 'command -v apt', returnStatus: true) == 0) {\n" +
                "                            env.AGENT_LABEL = 'my-agent-label'\n" +
                "                        } else if (sh(script: 'command -v brew', returnStatus: true) == 0) {\n" +
                "                            env.AGENT_LABEL = 'NodeHugh'\n" +
                "                        } else {\n" +
                "                            env.AGENT_LABEL = 'unknown-agent'\n" +
                "                        }\n" +
                "                    } else {\n" +
                "                        env.AGENT_LABEL = 'unknown-agent'\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('update package manager') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    if (isUnix()) {\n" +
                "                        def osType = sh(script: 'uname', returnStdout: true).trim()\n" +
                "                        if (osType == \"Darwin\") {\n" +
                "                            timeout(time: 1, unit: 'MINUTES') {\n" +
                "                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {\n" +
                "                                    sh 'brew update'\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                        else if (sh(script: 'command -v apt', returnStatus: true) == 0) {\n" +
                "                            timeout(time: 1, unit: 'MINUTES') {\n" +
                "                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {\n" +
                "                                    sh 'sudo apt update'\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                        else {\n" +
                "                            error \"Unsupported operating system\"\n" +
                "                        }\n" +
                "                    } else {\n" +
                "                        error \"Unsupported operating system\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Setup Environment') {\n" +
                "            steps{\n" +
                "                script {\n" +
                "                    def installDocker = {\n" +
                "                        if (sh(script: 'command -v apt', returnStatus: true) == 0) {\n" +
                "                            if (sh(script: 'docker --version', returnStatus: true) != 0) {\n" +
                "                                echo \"Docker is not installed. Installing Docker using apt.\"\n" +
                "                                echo \"Ubuntu detected. Installing Docker using apt.\"\n" +
                "                                sh '''\n" +
                "                                    sudo apt install -y apt-transport-https ca-certificates curl software-properties-common\n"
                +
                "                                    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -\n"
                +
                "                                    sudo add-apt-repository \"deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable\"\n"
                +
                "                                    sudo apt update\n" +
                "                                    sudo apt install -y docker-ce\n" +
                "                                    sudo service docker start\n" +
                "                                '''\n" +
                "                            }\n" +
                "                        } else if (sh(script: 'command -v yum', returnStatus: true) == 0) {\n" +
                "                            echo \"Amazon Linux detected. Installing Docker using yum.\"\n" +
                "                            sh '''\n" +
                "                                sudo yum update -y\n" +
                "                                sudo yum install -y docker\n" +
                "                                sudo systemctl start docker\n" +
                "                                sudo systemctl enable docker\n" +
                "                            '''\n" +
                "                        } else {\n" +
                "                            echo \"Unsupported OS. Cannot install Docker.\"\n" +
                "                        }\n" +
                "                    }\n" +
                "                    def osType = sh(script: 'uname', returnStdout: true).trim()\n" +
                "                    if (osType == \"Darwin\") {\n" +
                "                        def brewInstalled = sh(script: 'command -v brew', returnStatus: true) == 0\n" +
                "                        if (!brewInstalled) {\n" +
                "                            echo \"Homebrew not detected. Installing Homebrew first.\"\n" +
                "                            sh '''\n" +
                "                                /bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)\"\n"
                +
                "                            '''\n" +
                "                        }\n" +
                "                        if (sh(script: 'command -v brew', returnStatus: true) == 0) {\n" +
                "                            echo \"Homebrew installed. Installing Docker using brew.\"\n" +
                "                            sh '''\n" +
                "                                brew install --cask docker\n" +
                "                            '''\n" +
                "                        } else {\n" +
                "                            echo \"Failed to install Homebrew. You are not using MacOs.\"\n" +
                "                        }\n" +
                "                    } else {\n" +
                "                        retry(3, { installDocker() })\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Configure Docker Builder') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    sh 'export DOCKER_CLI_ACI=0'\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Check Docker Status') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    echo \"Check Docker Status\"\n" +
                "                    def status = sh(script: 'sudo docker info', returnStatus: true)\n" +
                "                    if (status != 0) {\n" +
                "                        def osType = sh(script: 'uname', returnStdout: true).trim()\n" +
                "                        if (osType == \"Darwin\") {\n" +
                "                            echo \"Docker is not running. Attempting to start Docker Desktop.\"\n" +
                "                            sh 'osascript -e \\'tell application \"Docker\" to activate\\''\n" +
                "                            sleep 10\n" +
                "                        }\n" +
                "                        else{\n" +
                "                            echo \"Docker is not running. Attempting to start Docker.\"\n" +
                "                            sh 'sudo dockerd &'\n" +
                "                            sleep 10\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Setup Docker Environment for Python') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def osType = sh(script: 'uname', returnStdout: true).trim()\n" +
                "                    if (osType == \"Darwin\") {\n" +
                "                        def pythonCheck = sh(script: 'python3 --version', returnStatus: true)\n" +
                "                        if (pythonCheck != 0) {\n" +
                "                            echo 'Python not found, installing...'\n" +
                "                            sh  \n" +
                "                            '''\n" +
                "                                brew install python3\n" +
                "                            '''\n" +
                "                        } else {\n" +
                "                            echo 'Python already installed.'\n" +
                "                        }\n" +
                "                        def pipCheck = sh(script: 'pip --version', returnStatus: true)\n" +
                "                        if (pipCheck != 0) {\n" +
                "                            echo 'pip3 not found, installing...'\n" +
                "                            sh '''\n" +
                "                                curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py\n" +
                "                                python3 get-pip.py\n" +
                "                            '''\n" +
                "                        } else {\n" +
                "                            echo 'pip3 already installed.'\n" +
                "                        }\n" +
                "                    } else {\n" +
                "                        // Linux setup - You can keep your existing Linux Docker setup here\n" +
                "                        def dockerfileContent = \"\"\"\n" +
                "                            # Start with a Node.js base image\n" +
                "                            FROM node:16.14 AS node-base\n" +
                "                            RUN npm cache clean -f\n" +
                "                            RUN npm install -g pnpm snyk@1.1229.0\n" +
                "                            FROM python:3.11\n" +
                "                            RUN apt-get update && apt-get install -y python3-venv nodejs npm\n" +
                "                            RUN npm install -g snyk@1.1229.0\n" +
                "                            RUN apt-get update && apt-get install -y python3-venv\n" +
                "                            RUN python3 -m venv /workspace/PythonPlantsVsZombies/myenv\n" +
                "                            COPY PythonPlantsVsZombies/Requirements.txt /workspace/PythonPlantsVsZombies/Requirements.txt\n"
                +
                "                            RUN /bin/bash -c \"source /workspace/PythonPlantsVsZombies/myenv/bin/activate && pip install -r /workspace/PythonPlantsVsZombies/Requirements.txt\"\n"
                +
                "                        \"\"\"\n" +
                "                        writeFile file: 'Dockerfile', text: dockerfileContent\n" +
                "                        sh 'echo $SUDO_PASS | sudo -S docker build -t custom-python-snyk-image -f Dockerfile .'\n"
                +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Build Docker Image') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def osType = sh(script: 'uname', returnStdout: true).trim()\n" +
                "                    if (osType != \"Darwin\") {\n" +
                "                        sh 'docker build -t custom-python-snyk-image -f Dockerfile .'\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Check PythonPlantsVsZombies in Docker') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def osType = sh(script: 'uname', returnStdout: true).trim()\n" +
                "                    if (osType != \"Darwin\") {\n" +
                "                        def dirExists = sh(script: 'docker run --rm -v $(pwd):/workspace -w /workspace custom-python-snyk-image sh -c \"[ -d PythonPlantsVsZombies ] && echo exists || echo not exists\"', returnStdout: true).trim()\n"
                +
                "                        if (dirExists == \"exists\") {\n" +
                "                            echo \"PythonPlantsVsZombies directory exists inside the Docker container.\"\n"
                +
                "                            sh 'docker run --rm -v $(pwd):/workspace -w /workspace custom-python-snyk-image sh -c \"ls -la PythonPlantsVsZombies\"'\n"
                +
                "                        } else {\n" +
                "                            echo \"PythonPlantsVsZombies directory does not exist inside the Docker container.\"\n"
                +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Create Branch') {\n" +
                "            steps {\n" +
                "            script {\n" +
                "                sh '''if git rev-parse --verify feature/update-date >/dev/null 2>&1; then\n" +
                "                        git checkout feature/update-date\n" +
                "                    else\n" +
                "                        git checkout -b feature/update-date\n" +
                "                    fi'''\n" +
                "            }\n" +
                "        }\n" +
                "        }\n" +
                "        stage('Run Snyk Test First Time') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def osType = sh(script: 'uname', returnStdout: true).trim()\n" +
                "                    if (osType == \"Darwin\"){\n" +
                "                        def startTime = System.currentTimeMillis()\n" +
                "                        sh '''\n" +
                "                            cd PythonPlantsVsZombies\n" +
                "                            pwd\n" +
                "                            ls\n" +
                "                            python3 -m venv myenv\n" +
                "                            source myenv/bin/activate\n" +
                "                            pip install -r Requirements.txt\n" +
                "                            snyk auth ${SNYK_TOKEN}\n" +
                "                            snyk test --package-manager=pip --file=Requirements.txt > snyk_test_output.txt\n"
                +
                "                            cat snyk_test_output.txt\n" +
                "                            exit 0\n" +
                "                        '''\n" +
                "                        def duration = (System.currentTimeMillis() - startTime) / 1000\n" +
                "                        echo \"First Snyk scan took ${duration} seconds\"\n" +
                "                        env.SNYK_SCAN_DURATION= \"${duration}\"\n" +
                "                    } else {\n" +
                "                        def startTime = System.currentTimeMillis()\n" +
                "                        sh '''\n" +
                "                            docker run --rm -v $(pwd):/workspace -w /workspace -e SNYK_TOKEN=${SNYK_TOKEN} custom-python-snyk-image sh -c \"\n"
                +
                "                                cd PythonPlantsVsZombies\n" +
                "                                python3 -m venv myenv\n" +
                "                                source myenv/bin/activate\n" +
                "                                pip install -r Requirements.txt\n" +
                "                                snyk auth ${SNYK_TOKEN}\n" +
                "                                snyk test --package-manager=pip --file=Requirements.txt > snyk_test_output.txt\n"
                +
                "                                cat snyk_test_output.txt\n" +
                "                                exit 0\n" +
                "                        '''\n" +
                "                        def duration = (System.currentTimeMillis() - startTime) / 1000\n" +
                "                        echo \"First Snyk scan took ${duration} seconds\"\n" +
                "                        env.SNYK_SCAN_DURATION= \"${duration}\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "stage('Display Snyk Output') {\n" +
                "    steps {\n" +
                "        script {\n" +
                "            sh('''\n" +
                "                ls -la\n" +
                "                if [ ! -f PythonPlantsVsZombies/snyk_test_output.txt ]; then\n" +
                "                    touch PythonPlantsVsZombies/snyk_test_output.txt\n" +
                "                fi\n" +
                "                cat PythonPlantsVsZombies/snyk_test_output.txt\n" +
                "            ''')\n" +
                "        }\n" +
                "    }\n" +
                "}\n"
                +

                "        stage('Push Results to Pushgateway') {\n" +
                "            steps {\n" +
                "              script {\n" +
                "    def globalCounter = 0\n" +
                "    def vulnerabilitiesDetected = sh(script: \"" + grepCommand + "\", returnStatus: true) == 0\n" +
                "            def totalVulnerabilities = ['Low': 0, 'Medium': 0, 'High': 0, 'Critical': 0]\n" +
                "            if (!vulnerabilitiesDetected) {\n" +
                "                echo \"No vulnerabilities detected.\"\n" +
                "            } else {\n" +
                "                def ratings = sh(script: \"grep -oE '\\\\[(Low|Medium|High|Critical) Severity\\\\]' PythonPlantsVsZombies/snyk_test_output.txt | cut -d'[' -f2 | cut -d' ' -f1 | sort | uniq -c\", returnStdout: true).trim()\n"
                +
                "                ratings.split(\"\\n\").each { line ->\n" +
                "                    def (count, rating) = line.split()\n" +
                "                    totalVulnerabilities[rating] += count.toInteger()\n" +
                "                        }\n" +
                "                        def issuesCounter = 0;\n" +
                "                        ['Low', 'Medium', 'High', 'Critical'].each { severity ->\n" +
                "        def issues = sh(script: \"" + generateGrepCommand() +
                "\", returnStdout: true).trim();\n" +
                generateScript3() +
                "                        \n" +
                "                    }\n" +
                generateScript() + "\n" +
                "                    def weightMap = [\n" +
                "                        'Low': 1,\n" +
                "                        'Medium': 2,\n" +
                "                        'High': 5,\n" +
                "                        'Critical': 10\n" +
                "                    ]\n" +
                "                    def totalWeightedScore = 0\n" +
                "                    totalVulnerabilities.each { rating, count ->\n" +
                "                        def weightedScore = weightMap[rating] * count\n" +
                "                        totalWeightedScore += weightedScore\n" +
                "                    }\n" +
                "                    echo \"Pushing total weighted score for vulnerabilities: ${totalWeightedScore}\"\n"
                +
                " '''\n" +
                generateScript2() +
                "        }\n" +
                "                    def syncScanDurationCounter = 0;\n" +
                "                    echo \"Pushing Snyk scan duration: ${env.SNYK_SCAN_DURATION} seconds\"\n" +
                generateScript4() + "\n" +
                "                    syncScanDurationCounter++\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Send Deployment Frequency Commits to Python Plants Vs Zombies') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def commitCountInDir = sh(script: 'git log --oneline --all --since=\"1 day ago\" -- PythonPlantsVsZombies/ | wc -l', returnStdout: true).trim()\n"
                +
                "sh '''\n" +
                "echo 'git_commit_frequency_daily_PythonPlantsVsZombies ${commitCountInDir}' | curl --data-binary "
                + "'@-' " + "http://" + "$" + "{" + "JENKINS_HOST" + "}" + ":9091/metrics/job/git_metrics\n" +
                "'''" +

                "                    \"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Fail if vulnerable') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def vulnerabilitiesDetected = sh(script:" +
                "                        grep -qE '\\\\[(Low|Medium|High|Critical) Severity\\\\]' PythonPlantsVsZombies/snyk_test_output.txt"
                +
                "                    \", returnStatus: true) == 0\n" +
                "                    if (vulnerabilitiesDetected) {\n" +
                "                        echo \"failed due to vulnerabilities being detected.\"\n" +
                "                        exit 1\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Copy to Production / Merge') {\n" +
                "            steps {\n" +
                "            script {\n" +
                "                withCredentials([usernamePassword(credentialsId: 'Test', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {\n"
                +
                "sh '''\n" +
                "git config credential.helper 'store --file=.git/credentials'\n" +
                "echo " + "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.cs.adelaide.edu.au" + " > .git/credentials\n"
                +
                "'''"

                +
                "                        git checkout feature/update-date\n" +
                "                        if [ ! -d \"production\" ]; then\n" +
                "                            mkdir production\n" +
                "                        fi\n" +
                "                        if [ -d \"PythonPlantsVsZombies\" ]; then\n" +
                "                            if [ -f \"PythonPlantsVsZombies/package.json\" ]; then\n" +
                "                                echo \"package.json detected inside PythonPlantsVsZombies. Removing it before copying to production.\"\n"
                +
                "                                rm PythonPlantsVsZombies/package.json\n" +
                "                            fi\n" +
                "                            if [ -f \"PythonPlantsVsZombies/package-lock.json\" ]; then\n" +
                "                                echo \"package-lock.json detected inside PythonPlantsVsZombies. Removing it before copying to production.\"\n"
                +
                "                                rm PythonPlantsVsZombies/package-lock.json\n" +
                "                            fi\n" +
                "                            cp -r PythonPlantsVsZombies/* production/\n" +
                "                            echo \"last commit: $(date)\" > production/last_commit.txt\n" +
                "                            git add production/\n" +
                "                            git commit -m \"Copy PythonPlantsVsZombies to production directory and update last_commit.txt\"\n"
                +
                "                            git checkout main\n" +
                "                            git pull origin main\n" +
                "                            git merge feature/update-date --no-ff --strategy-option theirs -m \"Merge feature/update-date into main\"\n"
                +
                "                            git push origin main\n" +
                "                        else\n" +
                "                            echo \"Error: PythonPlantsVsZombies directory not found!\"\n" +
                "                            exit 1\n" +
                "                        fi\n" +
                "                    '''\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        stage('Send Deployment Frequency Merges') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    def mergeCount = sh(script: 'git log --oneline --merges --since=\"1 day ago\" origin/main | wc -l', returnStdout: true).trim()\n"
                +
                "sh '''\n" +
                "    echo 'git_deployment_frequency_daily ${mergeCount}' | curl --data-binary @- http://${JENKINS_HOST}:9091/metrics/job/git_metrics\n"
                +
                "'''\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

        try {
            Jenkins jenkins = Jenkins.get();
            WorkflowJob job = new WorkflowJob(jenkins, "MyUniquePipelineJob");

            job.setDefinition(new CpsFlowDefinition(hardcodedScript, true));
            job.scheduleBuild2(0);
            listener.getLogger().println("Jenkinsfile executing as a Pipeline job.");
            // Wait for the completion of the scheduled job
            QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0);

            WorkflowRun WorkflowRun = future.waitForStart();

            while (WorkflowRun.getResult() == null) {
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
