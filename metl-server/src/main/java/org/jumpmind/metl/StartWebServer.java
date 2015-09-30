package org.jumpmind.metl;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;

public class StartWebServer {
    
    public static final int PORT = 42000;

    public static void main(String[] args) throws Exception {

        System.out.println(IOUtils.toString(StartWebServer.class.getResource("/Metl.asciiart")));

        Server server = new Server(PORT);
        
        ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");
        
        MBeanContainer mbContainer = new MBeanContainer(
                ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);
 
        ProtectionDomain protectionDomain = StartWebServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();       
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/metl");
        webapp.setWar(location.toExternalForm());
        webapp.addAliasCheck(new AllowSymLinkAliasChecker());
        webapp.addServlet(DefaultServlet.class, "/*");
        
        server.setHandler(webapp);
        server.start();
        
        Logger.getLogger(StartWebServer.class.getName()).info("To use Metl, navigate to http://localhost:" + PORT + "/metl/app");
        
        server.join();
    }

}
