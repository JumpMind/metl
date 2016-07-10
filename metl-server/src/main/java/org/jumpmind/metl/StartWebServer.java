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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.server.ServerContainer;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class StartWebServer {

    public static final int PORT = 42000;

    public static void main(String[] args) throws Exception {
        runWebServer();
    }

    protected static void disableJettyLogging() {
        System.setProperty("org.eclipse.jetty.util.log.class", JavaUtilLog.class.getName());
        Logger.getLogger(JavaUtilLog.class.getName()).setLevel(Level.SEVERE);
        Logger rootLogger = Logger.getLogger("org.eclipse.jetty");
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.SEVERE);
        }
        rootLogger.setLevel(Level.SEVERE);
    }

    public static void runWebServer() throws Exception {

        disableJettyLogging();

        System.out.println(IOUtils.toString(StartWebServer.class.getResource("/Metl.asciiart")));

        Server server = new Server(PORT);

        ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);

        ProtectionDomain protectionDomain = StartWebServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        File warFile = new File(location.getFile());
        File locationDir = warFile.getParentFile();

        extractPlugins(warFile);

        WebAppContext webapp = new WebAppContext();

        // HashSessionManager sessionManager = new HashSessionManager();
        // File storeDir = new File(locationDir, "sessions");
        // storeDir.mkdirs();
        // sessionManager.setStoreDirectory(storeDir);
        // sessionManager.setLazyLoad(true);
        // sessionManager.setSavePeriod(5);
        // sessionManager.setDeleteUnrestorableSessions(true);
        // SessionHandler sessionHandler = new SessionHandler(sessionManager);
        // webapp.setSessionHandler(sessionHandler);

        webapp.setContextPath("/metl");
        webapp.setWar(location.toExternalForm());
        webapp.addAliasCheck(new AllowSymLinkAliasChecker());

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
        webapp.setExtraClasspath(extraClasspath.toString());

        server.setHandler(webapp);

        ServerContainer webSocketServer = WebSocketServerContainerInitializer.configureContext(webapp);
        webSocketServer.setDefaultMaxSessionIdleTimeout(10000000);

        server.start();

        if (extraClasspath.length() > 0) {
            getLogger().info("Adding extra classpath of: " + extraClasspath.toString());
        }
        getLogger().info("To use Metl, navigate to http://localhost:" + PORT + "/metl");

        server.join();
    }

    private static void extractPlugins(File warFile) throws IOException {
        JarFile z = new JarFile(warFile);
        try {
            Enumeration<JarEntry> entries = z.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("plugins")) {
                    File f = new File(entry.getName());
                    if (entry.isDirectory()) {
                        f.mkdirs();
                    } else if (!f.exists()) {
                        System.out.println("Extracting " + entry.getName());
                        f.getParentFile().mkdirs();
                        final InputStream is = z.getInputStream(entry);
                        final OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                        try {
                            final byte buffer[] = new byte[4096];
                            int readCount;
                            while ((readCount = is.read(buffer)) > 0) {
                                os.write(buffer, 0, readCount);
                            }
                        } finally {
                            os.close();
                            is.close();
                        }
                    }

                }
            }
        } finally {
            z.close();
        }
    }

    private final static Logger getLogger() {
        return Logger.getLogger(StartWebServer.class.getName());
    }
}
