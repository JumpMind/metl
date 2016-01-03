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

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationConfiguration.ClassInheritanceMap;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;

public class Develop {

    public static void main(String[] args) throws Exception {
        
        System.out.println(IOUtils.toString(new FileInputStream("../metl-server/src/main/resources/Metl.asciiart")));

        new File("working").mkdirs();
        System.setProperty("org.jumpmind.metl.ui.init.config.dir","working");
        
        Server server = new Server(42000);

        WebAppContext webapp = new WebAppContext();
        
//        HashSessionManager sessionManager = new HashSessionManager();
//        File storeDir = new File("working", "sessions");
//        storeDir.mkdirs();
//        sessionManager.setStoreDirectory(storeDir);
//        sessionManager.setLazyLoad(true);
//        sessionManager.setSavePeriod(5);
//        sessionManager.setDeleteUnrestorableSessions(true);
//        SessionHandler sessionHandler = new SessionHandler(sessionManager);
//        webapp.setSessionHandler(sessionHandler);
        
        webapp.setParentLoaderPriority(true);
        webapp.setConfigurationDiscovered(true);
        webapp.setContextPath("/metl");
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
