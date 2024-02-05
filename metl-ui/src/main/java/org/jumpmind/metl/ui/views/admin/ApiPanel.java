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
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.icon.VaadinIcon;

@UiComponent
@Scope(value = "ui")
@Order(400)
@AdminMenuLink(name = "REST", id = "REST", icon = VaadinIcon.WRENCH)
public class ApiPanel extends AbstractAdminPanel {

    final Log logger = LogFactory.getLog(getClass());

    private static final long serialVersionUID = 1L;

    public ApiPanel() {   
        setSizeFull();
        UI.getCurrent().getPage().fetchCurrentURL(url -> {
            String urlString = url.toString();
            IFrame e = new IFrame(urlString.substring(0, urlString.lastIndexOf("/")) + "/api.html");
            e.setSizeFull();
            add(e);
        });
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
    protected void refresh() {
    }

}
