# DORAAndSASTMetricJenkinsPlugin

Please download and read the documentation for up to date information (in folder CD1-main currently)
Here is section A.18 pertaining to the plugin as of 10/12/23

Please update the documentation word doc and only update this document to be in line with the word doc
(dont update this before updating the word, the word should be the most up to date source of documentation)

A.18 PLUGIN
To set up the Jenkins plugin the first step is to get Jenkins running on localhost.
Then install and set up maven.
In the demo plugin folder of the repository run:

mvn clean package -DskipTests 

This will generate the target file
Then you will need to add the hpi file through the advanced plugin menu 
 
Then click deploy.
Then restart the Jenkins instance.
Then create a new freestyle project
And add new build step: say hello world (current name of build step inherited from demo, will be modified)
We still need to configure the credentials properly at this step but this is when you would select them after fully configured
Then click save.
Running this freestyle project will generate a pipeline which runs the jenkinsfile created for this plugin.

------- main files of note -------------------------------------------------------------------------
Path\To\Project\DORAAndSASTMetricJenkinsPlugin\CD1-main\JenkinsPlugin\demo-plugin\src\main\java\io\jenkins\plugins\sample\HelloWorldBuilder.java
Contains the java code for the plugin

Path\To\Project\DORAAndSASTMetricJenkinsPlugin\CD1-main\JenkinsPlugin\demo-plugin\src\main\resources\jenkinsfile
Is the Jenkinsfile we are loading with all the relevant Jenkinsfile code

Path\To\Project\DORAAndSASTMetricJenkinsPlugin\CD1-main\JenkinsPlugin\demo-plugin\src\main\resources\io\jenkins\plugins\sample\HelloWorldBuilder\config.jelly
Defines the drop downs which connect to our java code


------ please add to files of note as you understand how the plugin works more, file structure understanding not yet complete -----------------------------------------------------------------------------------

