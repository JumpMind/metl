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

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;

@ParentLayout(MainLayout.class)
public class PageNotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    private static final long serialVersionUID = 1L;

    Span pageNotFoundSpan = new Span();

    public PageNotFoundView() {
        setSizeFull();
        add(pageNotFoundSpan);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        UI ui = UI.getCurrent();
        ui.getPage().fetchCurrentURL(url -> {
            String path = url.getPath();
            if (!StringUtils.remove(path, "/").equalsIgnoreCase("metlapp")) {
                pageNotFoundSpan.getElement().getThemeList().add("badge error");
                Icon errorIcon = new Icon(VaadinIcon.BAN);
                errorIcon.getStyle().set("padding", "var(--lumo-space-xs)");
                pageNotFoundSpan.add(errorIcon, new Span("Could not find page for " + path));
            }
        });
        return HttpServletResponse.SC_NOT_FOUND;
    }

}
