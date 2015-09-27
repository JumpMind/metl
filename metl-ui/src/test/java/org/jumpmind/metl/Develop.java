package org.jumpmind.metl;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationConfiguration.ClassInheritanceMap;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

public class Develop {

    public static void main(String[] args) throws Exception {
        Server server = new Server(42000);

        WebAppContext webapp = new WebAppContext();
        webapp.setParentLoaderPriority(true);
        webapp.setConfigurationDiscovered(true);
        webapp.setWar("src/main/webapp");
        webapp.setResourceBase("src/main/webapp");
        webapp.addServlet(DefaultServlet.class, "/*");

        ConcurrentHashMap<String, ConcurrentHashSet<String>> map = new ClassInheritanceMap();
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("org.jumpmind.metl.ui.init.AppInitializer");
        map.put("org.springframework.web.WebApplicationInitializer", set);
        webapp.setAttribute(AnnotationConfiguration.CLASS_INHERITANCE_MAP, map);

        webapp.setConfigurations(new Configuration[] { new AnnotationConfiguration() });

        server.setHandler(webapp);
        server.start();
        server.join();

    }

}
