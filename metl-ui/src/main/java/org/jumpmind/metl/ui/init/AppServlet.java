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
package org.jumpmind.metl.ui.init;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinServlet;

//@VaadinServletConfiguration(productionMode = false, ui = AppUI.class, widgetset = "org.jumpmind.metl.ui.AppWidgetSet")
//@WebServlet(urlPatterns = "/*", asyncSupported = true, initParams = {
//        @WebInitParam(name = "org.atmosphere.cpr.asyncSupport", value = "org.atmosphere.container.JSR356AsyncSupport"),
//        @WebInitParam(name = "beanName", value = "appUI"), @WebInitParam(name = "productionMode", value = "false") })
public class AppServlet extends VaadinServlet {

    private static final long serialVersionUID = 1L;

    protected static final Logger log = LoggerFactory.getLogger(AppServlet.class);

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        getService().addSessionInitListener(new AppSessionInitListener());
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        super.service(request, response);
    }


}