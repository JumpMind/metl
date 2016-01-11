This is the development site for Metl.  To run the latest and greatest use the following instructions.  The latest milestone release and/or Users Guide can be downloaded  https://github.com/JumpMind/metl/releases/latest.

Metl is a simple, web-based integration platform that allows for several different styles of data integration including messaging, file based Extract/Transform/Load (ETL), and remote procedure invocation via Web Services. 

Metl was built to solve fairly simple day to day integration tasks without the need for custom coding, heavy infrastructure or high costs. It can be deployed in the cloud or in an internal data center, and was built to allow developers to extend it to fit their needs by writing their own components that can be included and leveraged by the existing Metl infrastructure.

<p align="center">
  <img src='https://raw.githubusercontent.com/wiki/JumpMind/metl/images/screenshots/design/design.png' />
</p>

## Build Me, Run Me

This project requires the Java JDK to build and run.  The build currently generates a war file 
which can be deployed to [Apache Tomcat](http://tomcat.apache.org).

### Build
~~~~~
cd metl-assemble
./gradlew assemble
~~~~~

### Run
~~~~~
cd ../metl-ui/build/libs
java -jar metl.war
~~~~~

### Deploy

To Tomcat:
~~~~~
cp ../metl-ui/build/libs/metl.war /opt/apache-tomcat-8.0.14/webapps/.
/opt/apache-tomcat-8.0.14/bin/catalina restart
~~~~~

Or install as a standalone service on Linux or Windows:
~~~~~
java -jar metl.war install
~~~~~


### Develop
To develop in [Eclipse](http://eclipse.org) run the following and import the projects:
~~~~~
cd metl-assemble
./gradlew develop
~~~~~

To run in Eclipse, use the "Run Metl" launch shortcut.
