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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Metl {

    public static void main(String[] args) throws Exception {
        disableLogging();
        if (args.length == 0) {
            StartWebServer.runWebServer();
        } else {
            Wrapper.runServiceWrapper(args);
        }
    }

    protected static void disableLogging() {
        System.setProperty("org.eclipse.jetty.util.log.class","org.eclipse.jetty.util.log.JavaUtilLog");
        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.WARNING);
        }
        rootLogger.setLevel(Level.WARNING);
    }

}
