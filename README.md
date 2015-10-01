This project builds a web application that is a visual integration environment.  It is currently in development and is slated for a milestone release in September of 2015.

## Build Me, Run Me

This project requires the Java JDK to build and run.  The build currently generates a war file 
which can be deployed to [Apache Tomcat](http://tomcat.apache.org).

To build:
~~~~~
cd metl-assemble
./gradlew war
~~~~~

To deploy standalone:
~~~~~
cd ../metl-ui/build/libs
java -jar metl.war
~~~~~


To deploy to Tomcat:
~~~~~
cp ../metl-ui/build/libs/metl.war /opt/apache-tomcat-8.0.14/webapps/.
/opt/apache-tomcat-8.0.14/bin/catalina restart
~~~~~

To develop in [Eclipse](http://eclipse.org) run the following and import the projects:
~~~~~
./gradlew develop
~~~~~
