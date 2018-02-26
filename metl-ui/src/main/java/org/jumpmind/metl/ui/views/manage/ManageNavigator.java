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
package org.jumpmind.metl.ui.views.manage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.AgentName;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.Name;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Icons;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class ManageNavigator extends Panel {

    protected static final Name CURRENTLY_RUNNING = new Name("Currently Running");
    
    protected static final Name IN_ERROR = new Name("In Error");

    TreeTable treeTable;

    Folder agentsFolder;

    Folder flowsFolder;
    
    ApplicationContext context;

    public ManageNavigator(FolderType folderType, ApplicationContext context) {
        this.context = context;

        setSizeFull();

        addStyleName(ValoTheme.MENU_ROOT);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);
        content.addComponent(leftMenuBar);

        treeTable = buildTreeTable();
        content.addComponent(treeTable);
        content.setExpandRatio(treeTable, 1);

        agentsFolder = new Folder();
        agentsFolder.setName("Agents");

        flowsFolder = new Folder();
        flowsFolder.setName("Flows");
    }

    public void refresh() {
        Object selected = treeTable.getValue();
        List<Object> expandedItems = new ArrayList<Object>();
        Collection<?> items = treeTable.getItemIds();
        for (Object object : items) {
            if (!treeTable.isCollapsed(object)) {
                expandedItems.add(object);
            }
        }

        treeTable.removeAllItems();
                
        treeTable.addItem(CURRENTLY_RUNNING);
        treeTable.setChildrenAllowed(CURRENTLY_RUNNING, false);
        treeTable.setItemIcon(CURRENTLY_RUNNING, FontAwesome.GEARS);
        
        treeTable.addItem(IN_ERROR);
        treeTable.setChildrenAllowed(IN_ERROR, false);
        treeTable.setItemIcon(IN_ERROR, FontAwesome.WARNING);

        
        treeTable.addItem(agentsFolder);
        treeTable.setItemIcon(agentsFolder, FontAwesome.FOLDER);

        List<Folder> folders = context.getConfigurationService().findFolders(null, FolderType.AGENT);
        for (Folder folder : folders) {
            addChildFolder(folder, agentsFolder);
        }
        
        addAgentsToFolder(null);

        treeTable.addItem(flowsFolder);
        treeTable.setItemIcon(flowsFolder, FontAwesome.FOLDER);
        addFlowsToFolder(flowsFolder);

        for (Object object : expandedItems) {
            treeTable.setCollapsed(object, false);
        }

        treeTable.focus();
        if (treeTable.containsId(selected)) {
            treeTable.setValue(selected);
        } else {
            treeTable.setValue(CURRENTLY_RUNNING);
        }
    }

    protected void addChildFolder(Folder folder, AbstractObject root) {
        treeTable.addItem(folder);
        treeTable.setItemIcon(folder, FontAwesome.FOLDER);
        treeTable.setCollapsed(folder, true);
        if (folder.getParent() != null) {
            treeTable.setParent(folder, folder.getParent());
        } else {
            treeTable.setParent(folder, root);
        }

        List<Folder> children = folder.getChildren();
        for (Folder child : children) {
            addChildFolder(child, root);
        }
        if (folder.getFolderType() == FolderType.AGENT) {
            addAgentsToFolder(folder);
        }

    }

    @SuppressWarnings("unchecked")
    protected Set<Object> getTableValues() {
        Set<Object> selectedIds = null;
        Object obj = treeTable.getValue();
        if (obj instanceof Set) {
            selectedIds = (Set<Object>) obj;
        } else {
            selectedIds = new HashSet<Object>(1);
            if (obj != null) {
                selectedIds.add(obj);
            }
        }
        return selectedIds;
    }

    protected TreeTable buildTreeTable() {
        final TreeTable table = new TreeTable();
        table.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        table.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setEditable(false);
        table.setContainerDataSource(new BeanItemContainer<AbstractNamedObject>(AbstractNamedObject.class));
        table.setVisibleColumns(new Object[] { "name" });
        table.setColumnExpandRatio("name", 1);

        table.addItemClickListener((event) -> {
            if (event.getButton() == MouseButton.LEFT) {
                if (event.isDoubleClick()) {
                    if (treeTable.hasChildren(event.getItemId())) {
                        treeTable.setCollapsed(event.getItemId(),
                                !treeTable.isCollapsed(event.getItemId()));
                    }
                }
            }
        });

        table.addCollapseListener((event) -> {
            if (event.getItemId() instanceof Folder) {
                table.setItemIcon(event.getItemId(), FontAwesome.FOLDER);
            }
        });

        table.addExpandListener((event) -> {
            if (event.getItemId() instanceof Folder) {
                table.setItemIcon(event.getItemId(), FontAwesome.FOLDER_OPEN);
            }
        });      

        table.setCellStyleGenerator(new CellStyleGenerator() {
            public String getStyle(Table source, Object itemId, Object propertyId) {
                if (itemId instanceof Folder && "name".equals(propertyId)) {
                    return "folder";
                } else {
                    return null;
                }
            }
        });
        
        table.setItemDescriptionGenerator((Component source, Object itemId, Object propertyId) -> {
            if (itemId instanceof ProjectVersionFlowName) {
                ProjectVersionFlowName flow = (ProjectVersionFlowName) itemId;
                return flow.projectVersion != null ? flow.projectVersion.getName() : "";
            } else if (itemId instanceof AgentDeploymentSummary) {
                AgentDeploymentSummary summary = (AgentDeploymentSummary) itemId;
                return summary.getProjectName();
            } else {
                return null;
            }
        });

        return table;
    }

    protected void addAgentsToFolder(Folder folder) {
        IOperationsService operationsService = context.getOperationsService();
        List<AgentName> agents = operationsService.findAgentsInFolder(folder);
        for (AgentName agent : agents) {

            List<AgentDeploymentSummary> deployments = operationsService.findAgentDeploymentSummary(agent.getId());

            treeTable.addItem(agent);
            treeTable.setItemIcon(agent, Icons.AGENT);
            treeTable.setChildrenAllowed(agent, deployments.size() > 0);
            treeTable.setParent(agent, folder != null ? folder : agentsFolder);

            for (AgentDeploymentSummary agentDeployment : deployments) {
                if (agentDeployment.getType().equals(AgentDeploymentSummary.TYPE_FLOW)) {
                    treeTable.addItem(agentDeployment);
                    treeTable.setItemIcon(agentDeployment, Icons.DEPLOYMENT);
                    treeTable.setParent(agentDeployment, agent);
                    treeTable.setChildrenAllowed(agentDeployment, false);
                }
            }
        }
    }

    protected void addFlowsToFolder(Folder folder) {
        IConfigurationService configurationService = context.getConfigurationService();
        IExecutionService executionService = context.getExecutionService();
        List<String> executedFlowIds = executionService.findExecutedFlowIds();
        List<FlowName> flows = configurationService.findFlows();
        Map<String, ProjectVersion> projectVersions = configurationService.findProjectVersions();
        for (FlowName flow : flows) {
            if (executedFlowIds.contains(flow.getId())) {
                flow = new ProjectVersionFlowName(projectVersions.get(flow.getProjectVersionId()), flow);
                treeTable.addItem(flow);
                treeTable.setItemIcon(flow, Icons.FLOW);
                treeTable.setParent(flow, folder);
                treeTable.setChildrenAllowed(flow, false);
            }
        }
    }

    public void addValueChangeListener(ValueChangeListener listener) {
        treeTable.addValueChangeListener(listener);
    }

    public Object getCurrentSelection() {
        return treeTable.getValue();
    }

    public Object getCurrentSelectionParent() {
        if (treeTable.getValue() != null) {
            return treeTable.getParent(treeTable.getValue());
        } else {
            return null;
        }
    }
    
    class ProjectVersionFlowName extends FlowName {
        
        ProjectVersion projectVersion;
        
        public ProjectVersionFlowName(ProjectVersion projectVersion, FlowName flowName) {
            super(flowName);
            this.projectVersion = projectVersion;
        }
        
    }

}
