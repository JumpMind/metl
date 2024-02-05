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

import static org.apache.commons.lang.StringUtils.isBlank;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;

public class PageNotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    private static final long serialVersionUID = 1L;

    Span pageNotFoundSpan = new Span();

    public PageNotFoundView() {
        setSizeFull();
        setMargin(true);
        add(pageNotFoundSpan);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
          ErrorParameter<NotFoundException> parameter) {
        UI.getCurrent().getPage().fetchCurrentURL(url -> {
            String uriFragment = url.getRef();
            if (isBlank(uriFragment)) {
                //viewManager.navigateToDefault();
            } else {
                pageNotFoundSpan.addClassName("failure");
                pageNotFoundSpan.setText("Could not find page for " + uriFragment);
            }
        });
        return HttpServletResponse.SC_NOT_FOUND;
    }

}
