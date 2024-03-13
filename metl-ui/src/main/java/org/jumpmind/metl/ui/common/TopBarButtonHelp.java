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
package org.jumpmind.metl.ui.common;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.spring.annotation.UIScope;

@Component
@UIScope
@Order(value=20)
public class TopBarButtonHelp extends TopBarButton {

    private static final long serialVersionUID = 1L;

    public TopBarButtonHelp() {
        super("Help", VaadinIcon.QUESTION_CIRCLE);
        addClickListener(event -> openHelp(event));
    }

    protected void openHelp(ClickEvent<Button> event) {
        Page page = UI.getCurrent().getPage();
        page.fetchCurrentURL(url -> {
            String docUrl = url.toString();
            docUrl = docUrl.substring(0, docUrl.lastIndexOf("/"));
            page.open(docUrl + "/doc/html/user-guide.html", "doc");
        });
    }

}
