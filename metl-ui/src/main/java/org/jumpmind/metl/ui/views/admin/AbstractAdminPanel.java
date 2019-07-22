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

import javax.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
@UiComponent
@UIScope
public abstract class AbstractAdminPanel extends VerticalLayout implements AdminSideView, IUiPanel {
   
    @Autowired
    protected ApplicationContext context;
 
    protected AdminView adminView;
    
    protected abstract void refresh();
    
    @PostConstruct 
    public void init() {
        refresh();
    }
    
    @Override
    public Component getView() {
        return this;
    }

    @Override
    public void setAdminView(AdminView view) {
        this.adminView = view;
    }
    
    public ApplicationContext getContext() {
        return this.context;
    }
    
    public AdminView getAdminView() {
        return this.adminView;
    }
    
    @Override
    public boolean isAccessible() {
        return true;
    }
}
