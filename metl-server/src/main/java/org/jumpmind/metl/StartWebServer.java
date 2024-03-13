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

import org.apache.commons.io.IOUtils;

public class StartWebServer {
    
    public static void main(String[] args) throws Exception {
        runWebServer(args);
    }

    public static void runWebServer(String[] args) throws Exception {
        new File(System.getProperty("java.io.tmpdir")).mkdirs();

        System.out.println(IOUtils.toString(StartWebServer.class.getResource("/Metl.asciiart")));

        MetlBoot.run(new String[0]);

        /*ClassList classlist = Configurations.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);

        ServerContainer webSocketServer = WebSocketServerContainerInitializer.configureContext(webapp);

        webSocketServer.setDefaultMaxSessionIdleTimeout(10000000);*/
    }
}
