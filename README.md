# Build Me, Run Me

This project requires the Java JDK to build and run.  The build currently generates a war file 
which can be deployed to [Apache Tomcat](http://tomcat.apache.org).

To build:
~~~~~
cd symmetric-is-assemble
./gradlew war
~~~~~

To deploy:
~~~~~
cp ../symmetric-is-ui/build/libs/symmetric-is.war /opt/apache-tomcat-8.0.14/webapps/.
/opt/apache-tomcat-8.0.14/bin/catalina restart
~~~~~

To develop in [Eclipse](http://eclipse.org) run the following and import the projects:
~~~~~
./gradlew develop
~~~~~
