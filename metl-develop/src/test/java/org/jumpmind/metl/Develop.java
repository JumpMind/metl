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

public class Develop {

    public static void main(String[] args) throws Exception {
        new File("working").mkdirs();
        System.setProperty("org.jumpmind.metl.ui.init.config.dir","working");

        /*ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");
        webapp.setWar(pathPrefix + "metl-war/src/main/webapp");
        webapp.setResourceBase(pathPrefix + "metl-war/src/main/webapp");
        
        ServerContainer webSocketServer = WebSocketServerContainerInitializer.configureContext(webapp);
        webSocketServer.setDefaultMaxSessionIdleTimeout(10000000);        */
        
        StartWebServer.runWebServer(new String[0]);

    }
    
    public static String getPathPrefix() {
        return "../";
    }

}
