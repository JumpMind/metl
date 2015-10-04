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
package org.jumpmind.metl.ui.views.deploy;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.symmetric.ui.common.ResizableWindow;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class FlowSelectWindow extends ResizableWindow {
    
    private static final long serialVersionUID = 1L;

    ApplicationContext context;
    
    Tree tree = new Tree();
    
    Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();
    
    FlowSelectListener listener;
    
    @SuppressWarnings({ "serial" })
    public FlowSelectWindow(ApplicationContext context, String caption, String introText) {
        super(caption);
        this.context = context;
        
        tree.setSizeFull();
        tree.setMultiSelect(true);
        tree.addContainerProperty("name", String.class, "");
        tree.setItemCaptionPropertyId("name");
        tree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        tree.addExpandListener(new ExpandListener() {
            public void nodeExpand(ExpandEvent event) {
                Object itemId = event.getItemId();
                if (itemId instanceof ProjectVersion) {
                    addFlowsToVersion((ProjectVersion) itemId);
                }
            }               
        });
        addProjects();
        
        setWidth(600.0f, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setSizeFull();
        
        HorizontalLayout hlayout = new HorizontalLayout();
        hlayout.setStyleName(ValoTheme.WINDOW_TOP_TOOLBAR);
        hlayout.setWidth(100f, Unit.PERCENTAGE);
        hlayout.addComponent(new Label(introText));
        layout.addComponent(hlayout);
        
        Panel scrollable = new Panel();
        scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
        scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        scrollable.setSizeFull();
        scrollable.setContent(tree);
        layout.addComponent(scrollable);
        layout.setExpandRatio(scrollable, 1.0f);
        addComponent(layout, 1);
        
        Button cancelButton = new Button("Cancel");
        Button selectButton = new Button("Select");
        addComponent(buildButtonFooter(cancelButton, selectButton));
        
        cancelButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                close();
            }
        });

        selectButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                Collection<FlowName> flowCollection = getFlowCollection();
                listener.selected(flowCollection);
                close();
            }
        });     
    }

    @SuppressWarnings("unchecked")
    protected Collection<FlowName> getFlowCollection() {
        Collection<FlowName> flowCollection = new HashSet<FlowName>();
        addFlowsToCollection(flowCollection, (Collection<Object>) tree.getValue());
        return flowCollection;
    }

    protected void addFlowsToCollection(Collection<FlowName> flowCollection, Collection<?> itemIds) {
        for (Object itemId : itemIds) {
            if (itemId instanceof FlowName) {
                flowCollection.add((FlowName) itemId);    
            } else {
                Collection<?> children = tree.getChildren(itemId);
                if (children == null) {
                    addFlowsToVersion((ProjectVersion) itemId);
                    children = tree.getChildren(itemId);
                }
                if (children != null) {
                    addFlowsToCollection(flowCollection, children);
                }
            }
        }
    }

    protected void addProjects() {
        List<Project> projects = context.getConfigurationService().findProjects();
        for (Project project : projects) {
            addItem(project, project.getName(), Icons.PROJECT, null, true);
            for (ProjectVersion version : project.getProjectVersions()) {
                addItem(version, version.getVersionLabel(), Icons.VERSION, project, true);
            }
        }
    }

    protected void addFlowsToVersion(ProjectVersion version) {
        List<FlowName> flows = context.getConfigurationService().findFlowsInProject(version.getId());
        for (FlowName flow : flows) {
            addItem(flow, flow.getName(), Icons.FLOW, version, false);
        }
    }

    @SuppressWarnings("unchecked")
    protected void addItem(Object itemId, String name, FontAwesome icon, Object parent, boolean areChildrenAllowed) {
        tree.addItem(itemId);
        tree.getContainerProperty(itemId, "name").setValue(name);
        tree.setItemIcon(itemId, icon);
        tree.setParent(itemId, parent);
        tree.setChildrenAllowed(itemId, areChildrenAllowed);
    }
    
    public void setFlowSelectListener(FlowSelectListener listener) {
        this.listener = listener;
    }

}