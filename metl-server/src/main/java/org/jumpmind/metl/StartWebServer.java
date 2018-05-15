/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.PrivateKey;
import java.security.ProtectionDomain;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.websocket.server.ServerContainer;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class StartWebServer {

    private final static String DEFAULT_HTTP_PORT = "42000";
    private final static String DEFAULT_HTTPS_PORT = "42443";

    private final static String HTTP_ENABLE = "http.enable";
    private final static String HTTP_PORT = "http.port";
    private final static String HTTP_HOST_BIND_NAME = "http.host.bind.name";

    private final static String HTTPS_ENABLE = "https.enable";
    private final static String HTTPS_PORT = "https.port";
    private final static String HTTPS_HOST_BIND_NAME = "https.host.bind.name";

    private final static String SERVER_ALLOW_DIR_LISTING = "server.allow.dir.list";
    private final static String SERVER_ALLOW_HTTP_METHODS = "server.allow.http.methods";
    private final static String SERVER_DISALLOW_HTTP_METHODS = "server.disallow.http.methods";

    private final static String SSL_KEYSTORE_FILE = "metl.keystore.file";
    private final static String SSL_TRUSTSTORE_FILE = "javax.net.ssl.trustStore";
    private final static String SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private final static String SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    private final static String SSL_IGNORE_PROTOCOLS = "metl.ssl.ignore.protocols";
    private final static String SSL_IGNORE_CIPHERS = "metl.ssl.ignore.ciphers";
    private final static String SSL_KEYSTORE_CERT_ALIAS = "metl.keystore.ssl.cert.alias";
    private final static String SSL_DEFAULT_ALIAS_PRIVATE_KEY = "metl";
    private final static String SSL_KEYSTORE_TYPE = "metl.keystore.type";
    private final static String SSL_DEFAULT_KEYSTORE_TYPE = "JCEKS";
    
    public static void main(String[] args) throws Exception {
        runWebServer(args);
    }

    public static void runWebServer(String[] args) throws Exception {
        disableJettyLogging();

        new File(System.getProperty("java.io.tmpdir")).mkdirs();

        System.out.println(IOUtils.toString(StartWebServer.class.getResource("/Metl.asciiart")));

        Server server = new Server();
        Connector[] connectors = getConnectors(args, server); 
        server.setConnectors(connectors);

        ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);

        ProtectionDomain protectionDomain = StartWebServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();

        String allowDirListing = System.getProperty(SERVER_ALLOW_DIR_LISTING, "false");
        String allowedMethods = System.getProperty(SERVER_ALLOW_HTTP_METHODS, "");
        String disallowedMethods = System.getProperty(SERVER_DISALLOW_HTTP_METHODS, "OPTIONS");

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/metl");
        if (location.toExternalForm().toLowerCase().endsWith(".war")) {
            webapp.setWar(location.toExternalForm());
        } else {
            webapp.setWar("web");
        }
        webapp.addAliasCheck(new AllowSymLinkAliasChecker());
        webapp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", allowDirListing);

        FilterHolder filterHolder = new FilterHolder(HttpMethodFilter.class);
        filterHolder.setInitParameter("server.allow.http.methods", allowedMethods);
        filterHolder.setInitParameter("server.disallow.http.methods", disallowedMethods);
        webapp.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

        String extraClasspath = getPluginClasspath(new File(Wrapper.getConfigDir(null, false)));
        webapp.setExtraClasspath(extraClasspath);
        if (extraClasspath.length() > 0) {
            getLogger().info("Adding extra classpath of: " + extraClasspath.toString());
        }

        server.setHandler(webapp);

        ServerContainer webSocketServer = WebSocketServerContainerInitializer.configureContext(webapp);

        webSocketServer.setDefaultMaxSessionIdleTimeout(10000000);

        server.start();
        
        server.join();
    }

    private static Connector[] getConnectors(String[] args, Server server) throws IOException {
        boolean httpEnabled = System.getProperty(HTTP_ENABLE, "true").equals("true");

        int httpPort = Integer.parseInt(System.getProperty(HTTP_PORT, DEFAULT_HTTP_PORT));
        String httpHostBindName = System.getProperty(HTTP_HOST_BIND_NAME, "0.0.0.0");
        int httpsPort = Integer.parseInt(System.getProperty(HTTPS_PORT, DEFAULT_HTTPS_PORT));

        boolean httpsEnabled = System.getProperty(HTTPS_ENABLE, "true").equals("true");
        String httpsHostBindName = System.getProperty(HTTPS_HOST_BIND_NAME, "0.0.0.0");

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(32768);

        List<Connector> connectors = new ArrayList<>(2);

        if (httpEnabled) {
            ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            http.setPort(httpPort);
            http.setHost(httpHostBindName);
            connectors.add(http);

            System.out.println(String.format("Metl can be reached on http://%s:%d/metl",
                    httpHostBindName != null ? httpHostBindName : "localhost", httpPort));
        }

        if (httpsEnabled) {
            String keyStorePassword = System.getProperty(SSL_KEYSTORE_PASSWORD, "changeit");

            installSslCertIfNecessary(args, keyStorePassword);

            SslContextFactory sslConnectorFactory = new SslContextFactory();
            sslConnectorFactory.setKeyManagerPassword(keyStorePassword);

            /* Prevent POODLE attack */
            String ignoredProtocols = System.getProperty(SSL_IGNORE_PROTOCOLS);
            if (ignoredProtocols != null && ignoredProtocols.length() > 0) {
                String[] protocols = ignoredProtocols.split(",");
                sslConnectorFactory.addExcludeProtocols(protocols);
            } else {
                sslConnectorFactory.addExcludeProtocols("SSLv3");
            }

            String ignoredCiphers = System.getProperty(SSL_IGNORE_CIPHERS);
            if (ignoredCiphers != null && ignoredCiphers.length() > 0) {
                String[] ciphers = ignoredCiphers.split(",");
                sslConnectorFactory.addExcludeCipherSuites(ciphers);
            }

            sslConnectorFactory.setCertAlias(System.getProperty(SSL_KEYSTORE_CERT_ALIAS, SSL_DEFAULT_ALIAS_PRIVATE_KEY));
            sslConnectorFactory.setKeyStore(getKeyStore(args, keyStorePassword));
            sslConnectorFactory.setTrustStore(getTrustStore(args));

            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(httpsPort);

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(sslConnectorFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
            https.setPort(httpsPort);
            https.setHost(httpsHostBindName);
            connectors.add(https);

            System.out.println(String.format("Metl can be reached on https://%s:%d/metl",
                    httpsHostBindName != null ? httpsHostBindName : "localhost", httpsPort));

        }
        
        System.out.println();

        return connectors.toArray(new Connector[connectors.size()]);
    }

    private static File getTrustStoreFile(String[] args) {
        return new File(System.getProperty(SSL_TRUSTSTORE_FILE, Wrapper.getConfigDir(args, false) + "/security/cacerts"));
    }

    private static File getKeyStoreFile(String[] args) {
        return new File(System.getProperty(SSL_KEYSTORE_FILE, Wrapper.getConfigDir(args, false) + "/security/keystore"));
    }

    private static String getHostName(String property) {
        final String UNKNOWN = "unknown";
        String hostName = System.getProperty(property, UNKNOWN);
        if (UNKNOWN.equals(hostName)) {
            try {
                hostName = System.getenv("HOSTNAME");

                if (isBlank(hostName)) {
                    hostName = System.getenv("COMPUTERNAME");
                }

                if (isBlank(hostName)) {
                    try {
                        hostName = IOUtils.toString(Runtime.getRuntime().exec("hostname").getInputStream());
                    } catch (Exception ex) {
                    }
                }

                if (isBlank(hostName)) {
                    hostName = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()).getHostName();
                }

                if (isNotBlank(hostName)) {
                    hostName = hostName.trim();
                }

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return hostName;
    }

    private static KeyStore getTrustStore(String[] args) {
        try {
            String keyStoreType = System.getProperty(SSL_KEYSTORE_TYPE, SSL_DEFAULT_KEYSTORE_TYPE);
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            File trustStoreFile = getTrustStoreFile(args);
            char[] password = System.getProperty(SSL_TRUSTSTORE_PASSWORD) != null ? System.getProperty(SSL_TRUSTSTORE_PASSWORD).toCharArray()
                    : null;
            if (trustStoreFile.exists()) {
                FileInputStream is = new FileInputStream(trustStoreFile);
                ks.load(is, password);
                is.close();
            } else {
                ks.load(null, password);
            }
            return ks;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyStore getKeyStore(String[] args, String keyPass) {
        try {
            String keyStoreType = System.getProperty(SSL_KEYSTORE_TYPE, SSL_DEFAULT_KEYSTORE_TYPE);
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            File keyStoreFile = getKeyStoreFile(args);
            if (keyStoreFile.exists()) {
                FileInputStream is = new FileInputStream(keyStoreFile);
                ks.load(is, keyPass.toCharArray());
                is.close();
            } else {
                ks.load(null, keyPass.toCharArray());
            }
            return ks;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void installSslCertIfNecessary(String[] args, String keyPass) {
        try {
            String hostName = getHostName(HTTPS_HOST_BIND_NAME);
            KeyStore keyStore = getKeyStore(args, keyPass);
            String alias = System.getProperty(SSL_KEYSTORE_CERT_ALIAS, SSL_DEFAULT_ALIAS_PRIVATE_KEY);
            KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(keyPass.toCharArray());
            Entry entry = keyStore.getEntry(alias, param);
            if (entry == null) {
                Class<?> keyPairClazz = Class.forName("sun.security.tools.keytool.CertAndKeyGen");
                Constructor<?> constructor = keyPairClazz.getConstructor(String.class, String.class);
                Object keypair = constructor.newInstance("RSA", "SHA1WithRSA");

                Class<?> x500NameClazz = Class.forName("sun.security.x509.X500Name");
                constructor = x500NameClazz.getConstructor(String.class, String.class, String.class, String.class, String.class,
                        String.class);
                Object x500Name = constructor.newInstance(hostName, "Metl", "JumpMind", "Unknown", "Unknown", "Unknown");

                keyPairClazz.getMethod("generate", Integer.TYPE).invoke(keypair, 1024);

                PrivateKey privKey = (PrivateKey) keyPairClazz.getMethod("getPrivateKey").invoke(keypair);

                X509Certificate[] chain = new X509Certificate[1];

                Date startDate = new Date(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
                long validTimeInMs = 100 * 365 * 24 * 60 * 60;
                chain[0] = (X509Certificate) keyPairClazz.getMethod("getSelfCertificate", x500NameClazz, Date.class, Long.TYPE)
                        .invoke(keypair, x500Name, startDate, validTimeInMs);

                keyStore.setKeyEntry(alias, privKey, keyPass.toCharArray(), chain);

                File keyStoreFile = getKeyStoreFile(args);
                keyStoreFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(keyStoreFile);
                keyStore.store(fos, keyPass.toCharArray());
                fos.close();
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void disableJettyLogging() {
        if (System.getProperty("enable.jetty.logging") == null) {
            LogManager.getLogManager().reset();
            System.setProperty("org.eclipse.jetty.util.log.class", JavaUtilLog.class.getName());
            Logger.getLogger(JavaUtilLog.class.getName()).setLevel(Level.SEVERE);
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

    private final static Logger getLogger() {
        return Logger.getLogger(StartWebServer.class.getName());
    }
}
