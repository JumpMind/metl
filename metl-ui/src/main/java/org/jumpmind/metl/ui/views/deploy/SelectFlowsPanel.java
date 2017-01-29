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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Icons;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SelectFlowsPanel extends VerticalLayout {
    
    private static final long serialVersionUID = 1L;

    ApplicationContext context;
    
    Tree tree = new Tree();
    
    Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();
    
    Project firstProject;
    
    public SelectFlowsPanel(ApplicationContext context, String introText,
    		boolean includeTestFlows) {
        this.context = context;
        
        tree.setMultiSelect(true);
        tree.addContainerProperty("name", String.class, "");
        tree.setItemCaptionPropertyId("name");
        tree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        tree.addExpandListener(event -> {
            Object itemId = event.getItemId();
            if (itemId instanceof ProjectVersion) {
                addFlowsToVersion((ProjectVersion) itemId, includeTestFlows);
            }
        });
        addProjects();
        
        setSpacing(true);
        setSizeFull();        
        addComponent(new Label(introText));
        
        Panel scrollable = new Panel();
        scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
        scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        scrollable.setSizeFull();
        scrollable.setContent(tree);
        addComponent(scrollable);
        setExpandRatio(scrollable, 1.0f);
                
        Set<Object> selected = new HashSet<>();
        if (firstProject != null) {
            selected.add(firstProject);
        }
        tree.setValue(selected);
        tree.focus();
    }

    @SuppressWarnings("unchecked")
    public Collection<FlowName> getSelectedFlows(boolean includeTestFlows) {
        Collection<FlowName> flowCollection = new TreeSet<FlowName>();
        addFlowsToCollection(flowCollection, (Collection<Object>) tree.getValue(), includeTestFlows);
        return flowCollection;
    }

    protected void addFlowsToCollection(Collection<FlowName> flowCollection, Collection<?> itemIds, boolean includeTestFlows) {
        for (Object itemId : itemIds) {
            if (itemId instanceof FlowName) {
                flowCollection.add((FlowName) itemId);    
            } else if (itemId instanceof ProjectVersion){
                Collection<?> children = tree.getChildren(itemId);
                if (children == null) {
                    addFlowsToVersion((ProjectVersion) itemId, includeTestFlows);
                    children = tree.getChildren(itemId);
                }
                if (children != null) {
                    addFlowsToCollection(flowCollection, children, includeTestFlows);
                }
            }
        }
    }

    protected void addProjects() {
        List<Project> projects = context.getConfigurationService().findProjects();
        for (Project project : projects) {
            if (firstProject == null) {
                firstProject = project;
            }
            addItem(project, project.getName(), Icons.PROJECT, null, true);
            for (ProjectVersion version : project.getProjectVersions()) {
                addItem(version, version.getVersionLabel(), Icons.VERSION, project, true);
            }
        }
    }

    protected void addFlowsToVersion(ProjectVersion version, boolean includeTestFlows) {
        List<FlowName> flows = context.getConfigurationService().findFlowsInProject(version.getId(), false);
        if (includeTestFlows) {
        	flows.addAll(context.getConfigurationService().findFlowsInProject(version.getId(), true));
        }
        Collections.sort(flows);
        for (FlowName flow : flows) {
            addItem(flow, flow.getName(), flow.isWebService() ? Icons.WEB : Icons.FLOW, version, false);
        }
        Set<Object> selected = new HashSet<>();
        selected.add(version);
        tree.setValue(selected);
        tree.focus();
    }

    @SuppressWarnings("unchecked")
    protected void addItem(Object itemId, String name, FontAwesome icon, Object parent, boolean areChildrenAllowed) {
        tree.addItem(itemId);
        tree.getContainerProperty(itemId, "name").setValue(name);
        tree.setItemIcon(itemId, icon);
        tree.setParent(itemId, parent);
        tree.setChildrenAllowed(itemId, areChildrenAllowed);
    }

}