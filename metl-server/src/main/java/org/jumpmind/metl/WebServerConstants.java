package org.jumpmind.metl;

import org.jumpmind.security.SecurityConstants;

final public class WebServerConstants {

    private WebServerConstants() {
    }
    
    public final static String SYSPROP_JMX_HTTP_CONSOLE_ENABLED = "jmx.http.console.for.embedded.webserver.enabled";
    public final static String SYSPROP_JMX_HTTP_CONSOLE_LOCALHOST_ENABLED = "jmx.http.console.localhost.only.enabled";
    public static final String SYSPROP_STANDALONE_WEB = "metl.standalone.web";
    public static final String SYSPROP_WEB_DIR = "metl.default.web.dir";
    public static final String SYSPROP_SERVER_PROPERTIES_PATH = "metl.server.properties.path";
    public static final String SYSPROP_CLUSTER_SERVER_ID = "runtime.metl.cluster.server.id";
    public static final String SYSPROP_DEFAULT_HTTP_PORT = "metl.default.http.port";
    public static final String SYSPROP_DEFAULT_HTTPS_PORT = "metl.default.https.port";
    public static final String SYSPROP_DEFAULT_JMX_PORT = "metl.default.jmx.port";
    public static final String SYSPROP_KEYSTORE_TYPE = SecurityConstants.SYSPROP_KEYSTORE_TYPE;
    public static final String SYSPROP_KEYSTORE_CERT_ALIAS = SecurityConstants.SYSPROP_KEYSTORE_CERT_ALIAS;
    
    public final static String HOST_BIND_NAME = "host.bind.name";

    public final static String HTTP_ENABLE = "http.enable";
    public final static String HTTP_PORT = "http.port";

    public final static String HTTPS_ENABLE = "https.enable";
    public final static String HTTPS_PORT = "https.port";
    
    public final static String HTTPS_VERIFIED_SERVERS = "https.verified.server.names";
    public final static String HTTPS_ALLOW_SELF_SIGNED_CERTS = "https.allow.self.signed.certs";    
    
    public final static String JMX_HTTP_ENABLE = "jmx.http.enable";
    public final static String JMX_HTTP_PORT = "jmx.http.port";

}
