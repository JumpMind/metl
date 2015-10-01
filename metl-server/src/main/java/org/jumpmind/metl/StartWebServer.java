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

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;

public class StartWebServer {
    
    public static final int PORT = 42000;

    public static void main(String[] args) throws Exception {

        System.out.println(IOUtils.toString(StartWebServer.class.getResource("/Metl.asciiart")));

        Server server = new Server(PORT);
        
        ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");
        
        MBeanContainer mbContainer = new MBeanContainer(
                ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);
 
        ProtectionDomain protectionDomain = StartWebServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();       
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/metl");
        webapp.setWar(location.toExternalForm());
        webapp.addAliasCheck(new AllowSymLinkAliasChecker());
        webapp.addServlet(DefaultServlet.class, "/*");
        
        server.setHandler(webapp);
        server.start();
        
        Logger.getLogger(StartWebServer.class.getName()).info("To use Metl, navigate to http://localhost:" + PORT + "/metl/app");
        
        server.join();
    }

}
