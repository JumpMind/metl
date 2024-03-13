package org.jumpmind.metl;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.SymlinkAllowedResourceAliasChecker;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

public class JettyCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory>, JettyServerCustomizer {
    Logger log = Logger.getLogger(JettyCustomizer.class.getName());
    
    @Override
    public void customize(JettyServletWebServerFactory factory) {
        factory.addServerCustomizers(this);
    }
    
    @Override
    public void customize(Server server) {
        Properties sysProps = System.getProperties();
        boolean httpEnabled = sysProps.getProperty(ServerConstants.HTTP_ENABLE, "true").equals("true");
        boolean httpsEnabled = sysProps.getProperty(ServerConstants.HTTPS_ENABLE, "true").equals("true");
        String ignoredProtocols = System.getProperty(ServerConstants.SSL_IGNORE_PROTOCOLS, "SSLv3");
        String ignoredCiphers = System.getProperty(ServerConstants.SSL_IGNORE_CIPHERS);
        if (httpsEnabled) {
            for (Connector connector : server.getConnectors()) {
                if (connector instanceof ServerConnector serverConnector) {
                    HttpConnectionFactory connectionFactory = serverConnector.getConnectionFactory(HttpConnectionFactory.class);
                    if (connectionFactory != null) {
                        HttpConfiguration httpsConfig = connectionFactory.getHttpConfiguration();
                        if (httpsConfig != null) {
                            httpsConfig.setSendServerVersion(false);
                        }
                    }
                }
                if (connector instanceof AbstractConnector) {
                    for (ConnectionFactory connectionFactory : ((AbstractConnector) connector).getConnectionFactories()) {
                        if (connectionFactory instanceof SslConnectionFactory) {
                            SslContextFactory sslContextFactory = ((SslConnectionFactory) connectionFactory).getSslContextFactory();
                            if (ignoredProtocols != null && ignoredProtocols.length() > 0) {
                                String[] protocols = ignoredProtocols.split(",");
                                sslContextFactory.addExcludeProtocols(protocols);
                            }
                            if (ignoredCiphers != null && ignoredCiphers.length() > 0) {
                                String[] ciphers = ignoredCiphers.split(",");
                                sslContextFactory.addExcludeCipherSuites(ciphers);
                            }
                        }
                    }
                }
            }
        }
        if (httpEnabled && httpsEnabled) {
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setSendServerVersion(false);
            ServerConnector connector = new ServerConnector(server);
            connector.addConnectionFactory(new HttpConnectionFactory(httpConfig));
            connector.setPort(Integer.valueOf(sysProps.getProperty(ServerConstants.HTTP_PORT, ServerConstants.DEFAULT_HTTP_PORT)));
            server.addConnector(connector);
        }
        String extraClasspath = null;
        try {
            extraClasspath = getPluginClasspath(new File(Wrapper.getConfigDir(null, false)));
        } catch (Exception e) {
            log.severe("Failed to get plugin classpath");
        }
        for (Handler handler : server.getHandlers()) {
            if (handler instanceof WebAppContext webapp) {
                webapp.setParentLoaderPriority(true);
                webapp.setConfigurationDiscovered(true);
                webapp.addAliasCheck(new SymlinkAllowedResourceAliasChecker(webapp));
                log.info("Adding extra classpath of: " + extraClasspath);
                webapp.setExtraClasspath(extraClasspath);
            }
        }
    }
    
    private static String getPluginClasspath(File locationDir) throws Exception {
        File pluginsDir = new File(locationDir, "plugins");
        pluginsDir.mkdirs();
        StringBuilder extraClasspath = new StringBuilder();
        File[] files = pluginsDir.listFiles();
        Arrays.sort(files);
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                extraClasspath.append(file.toURI().toURL().toExternalForm()).append(",");
            }
        }
        return extraClasspath.toString();
    }
}
