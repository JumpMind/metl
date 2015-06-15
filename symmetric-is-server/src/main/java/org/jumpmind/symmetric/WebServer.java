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
package org.jumpmind.symmetric;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationConfiguration.ClassInheritanceMap;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.HashedSession;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.security.ISecurityService;
import org.jumpmind.security.SecurityConstants;
import org.jumpmind.security.SecurityServiceFactory;
import org.jumpmind.security.SecurityServiceFactory.SecurityServiceType;
import org.jumpmind.symmetric.transport.TransportManagerFactory;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;

/**
 * Start up SymmetricDS through an embedded Jetty instance.
 *
 * @see SymmetricLauncher#main(String[])
 */
public class WebServer {

    protected static final Logger log = LoggerFactory.getLogger(WebServer.class);

    protected static final String DEFAULT_WEBAPP_DIR = System.getProperty(
            WebServerConstants.SYSPROP_WEB_DIR, AppUtils.getSymHome() + "/web");

    public static final String DEFAULT_HTTP_PORT = System.getProperty(
            WebServerConstants.SYSPROP_DEFAULT_HTTP_PORT, "31415");

    public static final String DEFAULT_JMX_PORT = System.getProperty(
            WebServerConstants.SYSPROP_DEFAULT_JMX_PORT, "31416");

    public static final String DEFAULT_HTTPS_PORT = System.getProperty(
            WebServerConstants.SYSPROP_DEFAULT_HTTPS_PORT, "31417");

    public static final int DEFAULT_MAX_IDLE_TIME = 7200000;

    /**
     * The type of HTTP connection to create for this SymmetricDS web server
     */
    public enum Mode {
        HTTP, HTTPS, MIXED;
    }

    private Server server;

    private WebAppContext webapp;

    protected boolean join = true;

    protected String webHome = "/";

    protected int maxIdleTime = DEFAULT_MAX_IDLE_TIME;

    protected boolean httpEnabled = true;

    protected int httpPort = Integer.parseInt(DEFAULT_HTTP_PORT);

    protected boolean httpsEnabled = false;

    protected int httpsPort = -1;

    protected boolean jmxEnabled = true;

    protected int jmxPort = Integer.parseInt(DEFAULT_JMX_PORT);

    protected String basicAuthUsername = null;

    protected String basicAuthPassword = null;

    protected String propertiesFile = null;

    protected String host = null;

    protected boolean noDirectBuffer = false;

    protected String webAppDir = DEFAULT_WEBAPP_DIR;

    protected String name = "SymmetricDS";

    protected String httpSslVerifiedServerNames = "all";

    protected boolean allowSelfSignedCerts = true;

    protected Class<? extends WebApplicationInitializer>[] registeredAppInitializers;

    public WebServer() {
        this(null, DEFAULT_WEBAPP_DIR);
    }

    public WebServer(String propertiesUrl) {
        this(propertiesUrl, DEFAULT_WEBAPP_DIR);
    }

    public WebServer(int maxIdleTime, String propertiesUrl) {
        this(propertiesUrl, DEFAULT_WEBAPP_DIR);
        this.maxIdleTime = maxIdleTime;
    }

    public WebServer(String webDirectory, int maxIdleTime, String propertiesUrl, boolean join,
            boolean noDirectBuffer) {
        this(propertiesUrl, webDirectory);
        this.maxIdleTime = maxIdleTime;
        this.join = join;
        this.noDirectBuffer = noDirectBuffer;
    }

    public WebServer(String propertiesUrl, String webappDir) {
        this.propertiesFile = propertiesUrl;
        this.webAppDir = webappDir;
        initFromProperties();
    }

    public void setRegisteredAppInitializers(
            Class<? extends WebApplicationInitializer>[] registeredAppInitializers) {
        this.registeredAppInitializers = registeredAppInitializers;
    }

    public Class<? extends WebApplicationInitializer>[] getRegisteredAppInitializers() {
        return registeredAppInitializers;
    }

    protected void initFromProperties() {
        TypedProperties serverProperties = new TypedProperties(System.getProperties());
        httpEnabled = serverProperties.is(WebServerConstants.HTTP_ENABLE,
                Boolean.parseBoolean(System.getProperty(WebServerConstants.HTTP_ENABLE, "true")));
        httpsEnabled = serverProperties.is(WebServerConstants.HTTPS_ENABLE,
                Boolean.parseBoolean(System.getProperty(WebServerConstants.HTTPS_ENABLE, "true")));
        jmxEnabled = serverProperties.is(WebServerConstants.JMX_HTTP_ENABLE, Boolean
                .parseBoolean(System.getProperty(WebServerConstants.JMX_HTTP_ENABLE, "true")));
        httpPort = serverProperties.getInt(WebServerConstants.HTTP_PORT,
                Integer.parseInt(System.getProperty(WebServerConstants.HTTP_PORT, "" + httpPort)));
        httpsPort = serverProperties
                .getInt(WebServerConstants.HTTPS_PORT,
                        Integer.parseInt(System.getProperty(WebServerConstants.HTTPS_PORT, ""
                                + httpsPort)));
        jmxPort = serverProperties.getInt(WebServerConstants.JMX_HTTP_PORT, Integer.parseInt(System
                .getProperty(WebServerConstants.JMX_HTTP_PORT, "" + jmxPort)));
        host = serverProperties.get(WebServerConstants.HOST_BIND_NAME,
                System.getProperty(WebServerConstants.HOST_BIND_NAME, host));
        httpSslVerifiedServerNames = serverProperties.get(
                WebServerConstants.HTTPS_VERIFIED_SERVERS, System.getProperty(
                        WebServerConstants.HTTPS_VERIFIED_SERVERS, httpSslVerifiedServerNames));
        allowSelfSignedCerts = serverProperties.is(
                WebServerConstants.HTTPS_ALLOW_SELF_SIGNED_CERTS, Boolean.parseBoolean(System
                        .getProperty(WebServerConstants.HTTPS_ALLOW_SELF_SIGNED_CERTS, ""
                                + allowSelfSignedCerts)));

    }

    public WebServer start(int httpPort, int jmxPort, String propertiesUrl) throws Exception {
        this.propertiesFile = propertiesUrl;
        return start(httpPort, jmxPort);
    }

    public WebServer start() throws Exception {
        if (httpPort > 0 && httpsPort > 0 && httpEnabled && httpsEnabled) {
            return startMixed(httpPort, httpsPort, jmxPort);
        } else if (httpPort > 0 && httpEnabled) {
            return start(httpPort, jmxPort);
        } else if (httpsPort > 0 && httpsEnabled) {
            return startSecure(httpsPort, jmxPort);
        } else {
            throw new IllegalStateException(
                    "Either an http or https port needs to be set before starting the server.");
        }
    }

    public WebServer start(int httpPort) throws Exception {
        return start(httpPort, 0, httpPort + 1, Mode.HTTP);
    }

    public WebServer start(int httpPort, int jmxPort) throws Exception {
        return start(httpPort, 0, jmxPort, Mode.HTTP);
    }

    public WebServer startSecure(int httpsPort, int jmxPort) throws Exception {
        return start(0, httpsPort, jmxPort, Mode.HTTPS);
    }

    public WebServer startMixed(int httpPort, int secureHttpPort, int jmxPort) throws Exception {
        return start(httpPort, secureHttpPort, jmxPort, Mode.MIXED);
    }

    public WebServer start(int httpPort, int securePort, int httpJmxPort, Mode mode)
            throws Exception {

        TransportManagerFactory.initHttps(httpSslVerifiedServerNames, allowSelfSignedCerts);

        // indicate to the app that we are in stand alone mode
        System.setProperty(WebServerConstants.SYSPROP_STANDALONE_WEB, "true");

        server = new Server();

        server.setConnectors(getConnectors(server, httpPort, securePort, mode));
        setupBasicAuthIfNeeded(server);

        webapp = new WebAppContext();
        webapp.setParentLoaderPriority(true);
        webapp.setConfigurationDiscovered(true);
        webapp.setContextPath(webHome);
        webapp.setWar(webAppDir);
        webapp.setResourceBase(webAppDir);
        webapp.addServlet(DefaultServlet.class, "/*");

        ConcurrentHashMap<String, ConcurrentHashSet<String>> map = new ClassInheritanceMap();
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        if (registeredAppInitializers != null) {
            for (Class<?> clazz : registeredAppInitializers) {
                set.add(clazz.getName());
            }
        }
        map.put(WebApplicationInitializer.class.getName(), set);
        webapp.setAttribute(AnnotationConfiguration.CLASS_INHERITANCE_MAP, map);

        webapp.setConfigurations(new Configuration[] { new AnnotationConfiguration() });

        SessionManager sessionManager = new SessionManager();
        sessionManager.setLazyLoad(true);
        sessionManager.setDeleteUnrestorableSessions(true);
        sessionManager.setMaxInactiveInterval(10 * 60);
        webapp.getSessionHandler().setSessionManager(sessionManager);

        webapp.getServletContext()
                .getContextHandler()
                .setMaxFormContentSize(
                        Integer.parseInt(System.getProperty(
                                "org.eclipse.jetty.server.Request.maxFormContentSize", "800000")));
        webapp.getServletContext()
                .getContextHandler()
                .setMaxFormKeys(
                        Integer.parseInt(System.getProperty(
                                "org.eclipse.jetty.server.Request.maxFormKeys", "100000")));
        server.setHandler(webapp);

        server.start();

        if (httpJmxPort > 0) {
            registerHttpJmxAdaptor(httpJmxPort);
        }

        if (join) {
            log.info("Joining the web server main thread");
            server.join();
        }

        return this;
    }

    protected ServletContext getServletContext() {
        return webapp != null ? webapp.getServletContext() : null;
    }

    protected void setupBasicAuthIfNeeded(Server server) {
        if (StringUtils.isNotBlank(basicAuthUsername)) {
            ConstraintSecurityHandler sh = new ConstraintSecurityHandler();

            Constraint constraint = new Constraint();
            constraint.setName(Constraint.__BASIC_AUTH);

            constraint.setRoles(new String[] { SecurityConstants.EMBEDDED_WEBSERVER_DEFAULT_ROLE });
            constraint.setAuthenticate(true);

            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec("/*");
            // sh.setConstraintMappings(new ConstraintMapping[] {cm});
            sh.addConstraintMapping(cm);

            sh.setAuthenticator(new BasicAuthenticator());

            HashLoginService loginService = new HashLoginService();
            loginService.putUser(basicAuthUsername, new Password(basicAuthPassword), null);
            sh.setLoginService(loginService);

            server.setHandler(sh);

        }
    }

    protected Connector[] getConnectors(Server server, int port, int securePort, Mode mode) {
        ArrayList<Connector> connectors = new ArrayList<Connector>();
        String keyStoreFile = System.getProperty(SecurityConstants.SYSPROP_KEYSTORE);
        String keyStoreType = System.getProperty(WebServerConstants.SYSPROP_KEYSTORE_TYPE,
                SecurityConstants.KEYSTORE_TYPE);

        HttpConfiguration httpConfig = new HttpConfiguration();
        if (mode.equals(Mode.HTTPS) || mode.equals(Mode.MIXED)) {
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(securePort);
        }

        httpConfig.setOutputBufferSize(32768);

        if (mode.equals(Mode.HTTP) || mode.equals(Mode.MIXED)) {
            ServerConnector http = new ServerConnector(server,
                    new HttpConnectionFactory(httpConfig));
            http.setPort(port);
            http.setHost(host);
            http.setIdleTimeout(maxIdleTime);
            connectors.add(http);
            log.info(String.format("About to start %s web server on host:port %s:%s", name,
                    host == null ? "default" : host, port));
        }
        if (mode.equals(Mode.HTTPS) || mode.equals(Mode.MIXED)) {
            ISecurityService securityService = SecurityServiceFactory.create(
                    SecurityServiceType.SERVER, new TypedProperties(System.getProperties()));
            securityService.installDefaultSslCert(host);
            String keyStorePassword = System
                    .getProperty(SecurityConstants.SYSPROP_KEYSTORE_PASSWORD);
            keyStorePassword = (keyStorePassword != null) ? keyStorePassword
                    : SecurityConstants.KEYSTORE_PASSWORD;
            SslContextFactory sslConnectorFactory = new SslContextFactory();
            sslConnectorFactory.setKeyStorePath(keyStoreFile);
            sslConnectorFactory.setKeyManagerPassword(keyStorePassword);
            /* Prevent POODLE attack */
            sslConnectorFactory.addExcludeProtocols("SSLv3");
            sslConnectorFactory.setCertAlias(System.getProperty(
                    WebServerConstants.SYSPROP_KEYSTORE_CERT_ALIAS,
                    SecurityConstants.ALIAS_SYM_PRIVATE_KEY));
            sslConnectorFactory.setKeyStoreType(keyStoreType);

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            ServerConnector https = new ServerConnector(server, new SslConnectionFactory(
                    sslConnectorFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfig));
            https.setPort(securePort);
            https.setIdleTimeout(maxIdleTime);
            https.setHost(host);
            connectors.add(https);
            log.info(String.format("About to start %s web server on secure host:port %s:%s", name,
                    host == null ? "default" : host, securePort));
        }
        return connectors.toArray(new Connector[connectors.size()]);
    }

    protected void registerHttpJmxAdaptor(int jmxPort) throws Exception {
        if (AppUtils.isSystemPropertySet(WebServerConstants.SYSPROP_JMX_HTTP_CONSOLE_ENABLED, true)
                && jmxEnabled) {
            log.info("Starting JMX HTTP console on port {}", jmxPort);
        }
    }

    protected void removeHttpJmxAdaptor() {
        if (AppUtils.isSystemPropertySet(WebServerConstants.SYSPROP_JMX_HTTP_CONSOLE_ENABLED, true)
                && jmxEnabled) {
        }
    }

    public void stop() throws Exception {
        if (server != null) {
            removeHttpJmxAdaptor();
            server.stop();
        }
    }

    public static void main(String[] args) throws Exception {
        new WebServer().start(8080, 8081);
    }

    public boolean isJoin() {
        return join;
    }

    public void setJoin(boolean join) {
        this.join = join;
    }

    public void setWebHome(String webHome) {
        this.webHome = webHome;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setHttpPort(int httpPort) {
        System.setProperty(WebServerConstants.HTTP_PORT, Integer.toString(httpPort));
        this.httpPort = httpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpsPort(int httpsPort) {
        System.setProperty(WebServerConstants.HTTPS_PORT, Integer.toString(httpsPort));
        this.httpsPort = httpsPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setBasicAuthPassword(String basicAuthPassword) {
        this.basicAuthPassword = basicAuthPassword;
    }

    public void setBasicAuthUsername(String basicAuthUsername) {
        this.basicAuthUsername = basicAuthUsername;
    }

    public void setWebAppDir(String webAppDir) {
        this.webAppDir = webAppDir;
    }

    public void setNoDirectBuffer(boolean noDirectBuffer) {
        this.noDirectBuffer = noDirectBuffer;
    }

    public boolean isNoDirectBuffer() {
        return noDirectBuffer;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    public void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public void setHttpsEnabled(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    class SessionManager extends HashSessionManager {

        @Override
        protected AbstractSession newSession(HttpServletRequest request) {
            return new Session(this, request);
        }

        @Override
        protected AbstractSession newSession(long created, long accessed, String clusterId) {
            return new Session(this, created, accessed, clusterId);
        }

        @Override
        protected synchronized HashedSession restoreSession(String idInCuster) {
            if (isNotBlank(idInCuster)) {
                return super.restoreSession(idInCuster);
            } else {
                return null;
            }
        }

        public HashedSession restoreSession(InputStream is, HashedSession session) throws Exception {
            DataInputStream di = new DataInputStream(is);

            String clusterId = di.readUTF();
            di.readUTF(); // nodeId

            long created = di.readLong();
            long accessed = di.readLong();
            int requests = di.readInt();

            if (session == null)
                session = (HashedSession) newSession(created, accessed, clusterId);
            session.setRequests(requests);

            int size = di.readInt();

            restoreSessionAttributes(di, size, session);

            try {
                int maxIdle = di.readInt();
                session.setMaxInactiveInterval(maxIdle);
            } catch (EOFException e) {
                log.debug("No maxInactiveInterval persisted for session " + clusterId, e);
            }

            return session;
        }

        private void restoreSessionAttributes(InputStream is, int size, HashedSession session)
                throws Exception {
            if (size > 0) {
                ObjectInputStream ois = new ObjectInputStream(is);
                for (int i = 0; i < size; i++) {
                    String key = ois.readUTF();
                    try {
                        Object value = ois.readObject();
                        session.setAttribute(key, value);
                    } catch (Exception ex) {
                        if (ex instanceof ClassCastException
                                || ex instanceof ClassNotFoundException) {
                            log.warn("Could not restore the '"
                                    + key
                                    + "' session object.  Code has probably changed.  The error message was: "
                                    + ex.getMessage());
                        } else {
                            log.error("Could not restore the '" + key + "' session object.", ex);
                        }
                    }
                }
            }
        }

    }

    class Session extends HashedSession {

        protected Session(HashSessionManager hashSessionManager, HttpServletRequest request) {
            super(hashSessionManager, request);
        }

        protected Session(HashSessionManager hashSessionManager, long created, long accessed,
                String clusterId) {
            super(hashSessionManager, created, accessed, clusterId);
        }

        @Override
        public synchronized void save(OutputStream os) throws IOException {
            DataOutputStream out = new DataOutputStream(os);
            out.writeUTF(getClusterId());
            out.writeUTF(getNodeId());
            out.writeLong(getCreationTime());
            out.writeLong(getAccessed());
            out.writeInt(getRequests());

            Enumeration<String> e = getAttributeNames();
            int count = 0;
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                Object obj = doGet(key);
                if (obj instanceof Serializable) {
                    count++;
                }
            }
            out.writeInt(count);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            e = getAttributeNames();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                Object obj = doGet(key);
                if (obj instanceof Serializable) {
                    oos.writeUTF(key);
                    oos.writeObject(obj);
                }
            }
            oos.flush();
            out.writeInt(getMaxInactiveInterval());
        }

    }

}
