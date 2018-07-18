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

import javax.websocket.server.ServerContainer;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationConfiguration.ClassInheritanceMap;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class Develop {

    public static void main(String[] args) throws Exception {
        String pathPrefix = "../";
        if (args.length > 0) {
            pathPrefix = args[0];
        }
        System.out.println(IOUtils.toString(new FileInputStream(pathPrefix + "metl-server/src/main/resources/Metl.asciiart")));

        new File(System.getProperty("java.io.tmpdir")).mkdirs();
        new File("working").mkdirs();
        System.setProperty("org.jumpmind.metl.ui.init.config.dir","working");
        
        Server server = new Server(42000);
        ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

        WebAppContext webapp = new WebAppContext();
        webapp.setParentLoaderPriority(true);
        webapp.setConfigurationDiscovered(true);
        webapp.setContextPath("/metl");
        webapp.setWar(pathPrefix + "metl-war/src/main/webapp");
        webapp.setResourceBase(pathPrefix + "metl-war/src/main/webapp");

        ConcurrentHashMap<String, ConcurrentHashSet<String>> map = new ClassInheritanceMap();
        ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
        set.add("org.jumpmind.metl.ui.init.AppInitializer");
        map.put("org.springframework.web.WebApplicationInitializer", set);
        webapp.setAttribute(AnnotationConfiguration.CLASS_INHERITANCE_MAP, map);

        server.setHandler(webapp);
        
        ServerContainer webSocketServer = WebSocketServerContainerInitializer.configureContext(webapp);
        webSocketServer.setDefaultMaxSessionIdleTimeout(10000000);        
        
        server.start();
        server.join();

    }
    
    public static String getPathPrefix() {
        return "../";
    }

}
