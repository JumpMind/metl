package org.jumpmind.metl;

public class ServerConstants {
    public final static String DEFAULT_HTTP_PORT = "42000";
    public final static String DEFAULT_HTTPS_PORT = "42443";

    public final static String HTTP_ENABLE = "http.enable";
    public final static String HTTP_PORT = "http.port";
    public final static String HTTP_HOST_BIND_NAME = "http.host.bind.name";

    public final static String HTTPS_ENABLE = "https.enable";
    public final static String HTTPS_PORT = "https.port";
    public final static String HTTPS_HOST_BIND_NAME = "https.host.bind.name";

    public final static String SERVER_ALLOW_DIR_LISTING = "server.allow.dir.list";
    public final static String SERVER_ALLOW_HTTP_METHODS = "server.allow.http.methods";
    public final static String SERVER_DISALLOW_HTTP_METHODS = "server.disallow.http.methods";

    public final static String SSL_KEYSTORE_FILE = "metl.keystore.file";
    public final static String SSL_TRUSTSTORE_FILE = "javax.net.ssl.trustStore";
    public final static String SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    public final static String SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    public final static String SSL_IGNORE_PROTOCOLS = "metl.ssl.ignore.protocols";
    public final static String SSL_IGNORE_CIPHERS = "metl.ssl.ignore.ciphers";
    public final static String SSL_KEYSTORE_CERT_ALIAS = "metl.keystore.ssl.cert.alias";
    public final static String SSL_DEFAULT_ALIAS_PRIVATE_KEY = "metl";
    public final static String SSL_KEYSTORE_TYPE = "metl.keystore.type";
    public final static String SSL_DEFAULT_KEYSTORE_TYPE = "JCEKS";
}
