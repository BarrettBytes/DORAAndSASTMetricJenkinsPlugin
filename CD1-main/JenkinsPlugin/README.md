TEach time you modify the plugin type:  mvn clean package -Dmaven.test.skip=true in the demo-plugin directory
ried to make work on ec2 instance, it's impossible, ec2 can't handle maven and crashes anytime a maven project tries to build
will need to have jenkins set up locally to work on this
Basic info:
https://www.jenkins.io/doc/developer/tutorial/create/
https://www.jenkins.io/doc/developer/tutorial/run/
