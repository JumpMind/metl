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

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.spring.annotation.UIScope;

@Component
@UIScope
@Order(value=20)
public class TopBarButtonHelp extends TopBarButton {

    private static final long serialVersionUID = 1L;

    public TopBarButtonHelp() {
        super("Help", FontAwesome.QUESTION_CIRCLE);
        addClickListener(event -> openHelp(event));
    }

    protected void openHelp(ClickEvent event) {
        String docUrl = Page.getCurrent().getLocation().toString();
        docUrl = docUrl.substring(0, docUrl.lastIndexOf("/"));
        Page.getCurrent().open(docUrl + "/doc/html/user-guide.html", "doc");
    }

}
