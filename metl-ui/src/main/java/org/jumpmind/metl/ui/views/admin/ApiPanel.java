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
package org.jumpmind.metl.ui.views.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.core.annotation.Order;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.BrowserFrame;

@UiComponent
@UIScope
@Order(400)
@AdminMenuLink(name = "REST", id = "REST", icon = FontAwesome.WRENCH)
public class ApiPanel extends AbstractAdminPanel {

    final Log logger = LogFactory.getLog(getClass());

    private static final long serialVersionUID = 1L;

    public ApiPanel() {   
        setSizeFull();
        String url = Page.getCurrent()
                .getLocation().getPath();
        BrowserFrame e = new BrowserFrame(null, new ExternalResource(url.substring(0, url.lastIndexOf("/")) + "/api.html"));
        e.setSizeFull();
        addComponent(e);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }

    @Override
    protected void refresh() {
    }

}
