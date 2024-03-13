package org.jumpmind.metl;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertiesPropertySource;

public class MetlBootPropertySetupListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Properties sysProps = System.getProperties();
        Properties bootProps = new Properties();
        bootProps.put("server.servlet.context-path", "/metl");
        bootProps.put("server.servlet.jsp.init-parameters.listings",
                sysProps.getOrDefault(ServerConstants.SERVER_ALLOW_DIR_LISTING, "false"));
        bootProps.put("server.servlet.session.cookie.http-only", Boolean.toString(true));
        String war = "web";
        URL location = StartWebServer.class.getProtectionDomain().getCodeSource().getLocation();
        if (location.toExternalForm().toLowerCase().endsWith(".war")) {
            war = location.toExternalForm();
        }
        bootProps.put("spring.web.resources.static-locations",
                "classpath:[/META-INF/resources/,/resources/,/static/],file:" + war);
        boolean httpEnabled = sysProps.getProperty(ServerConstants.HTTP_ENABLE, "true").equals("true");
        boolean httpsEnabled = sysProps.getProperty(ServerConstants.HTTPS_ENABLE, "true").equals("true");
        String httpPort = sysProps.getProperty(ServerConstants.HTTP_PORT, ServerConstants.DEFAULT_HTTP_PORT);
        String httpsPort = sysProps.getProperty(ServerConstants.HTTPS_PORT, ServerConstants.DEFAULT_HTTPS_PORT);
        if (httpEnabled && !httpsEnabled) {
            setIfNotBlank(ServerConstants.HTTP_HOST_BIND_NAME, "server.address", sysProps, bootProps);
            bootProps.put("server.port", httpPort);
        } else if (httpsEnabled) {
            setIfNotBlank(ServerConstants.HTTPS_HOST_BIND_NAME, "server.address", sysProps, bootProps);
            bootProps.put("server.port", httpsPort);
            /*bootProps.put("server.ssl.enabled", Boolean.toString(true));
            if (!httpEnabled) {
                bootProps.setProperty("server.servlet.session.cookie.secure", Boolean.toString(true));
            }
            bootProps.put("server.ssl.key-store", sysProps.getOrDefault(ServerConstants.SSL_KEYSTORE_FILE,
                    Wrapper.getConfigDir(new String[0], false) + "/security/keystore"));
            bootProps.put("server.ssl.key-store-password", sysProps.getOrDefault(
                    ServerConstants.SSL_KEYSTORE_PASSWORD, "changeit"));
            bootProps.put("server.ssl.key-store-type", sysProps.getOrDefault(ServerConstants.SSL_KEYSTORE_TYPE,
                    ServerConstants.SSL_DEFAULT_KEYSTORE_TYPE));
            bootProps.put("server.ssl.key-alias", sysProps.getOrDefault(ServerConstants.SSL_KEYSTORE_CERT_ALIAS,
                    ServerConstants.SSL_DEFAULT_ALIAS_PRIVATE_KEY));
            String truststorePath = String.valueOf(sysProps.getOrDefault(ServerConstants.SSL_TRUSTSTORE_FILE,
                    Wrapper.getConfigDir(new String[0], false) + "/security/cacerts"));
            File truststore = new File(truststorePath);
            if (truststore.exists()) {
                bootProps.put("server.ssl.trust-store", truststorePath);
                Object truststorePassword = sysProps.get(ServerConstants.SSL_TRUSTSTORE_PASSWORD);
                if (truststorePassword != null) {
                    bootProps.put("server.ssl.trust-store-password", truststorePassword);
                }
                bootProps.put("server.ssl.trust-store-type", sysProps.getOrDefault(ServerConstants.SSL_KEYSTORE_TYPE,
                        ServerConstants.SSL_DEFAULT_KEYSTORE_TYPE));
            }*/
        }
        bootProps.put("server.cookie.name", getCookieName(httpEnabled, httpsEnabled, httpPort, httpsPort));
        event.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("metlBootProps", bootProps));
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(new CookieManager());
        }
    }
    
    protected static String getCookieName(boolean httpEnable, boolean httpsEnable, String httpPort, String httpsPort) {
        StringBuilder sb = new StringBuilder("JSESSIONID_");
        if (httpsEnable) {
            sb.append(httpsPort);
        }
        if (httpEnable) {
            sb.append("_").append(httpPort);
        }
        return sb.toString();
    }
    
    protected void setIfNotBlank(String sysName, String bootName, Properties sysProps, Properties bootProps) {
        String value = sysProps.getProperty(sysName);
        if (StringUtils.isNotBlank(value)) {
            bootProps.put(bootName, value);
        }
    }
}
