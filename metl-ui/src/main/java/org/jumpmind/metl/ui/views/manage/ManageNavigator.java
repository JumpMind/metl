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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.AbstractNamedObject;
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

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.selection.SelectionListener;

@SuppressWarnings("serial")
public class ManageNavigator extends VerticalLayout {

    protected static final Name CURRENTLY_RUNNING = new Name("Currently Running");
    
    protected static final Name IN_ERROR = new Name("In Error");

    TreeGrid<AbstractNamedObject> treeGrid;

    Folder agentsFolder;

    Folder flowsFolder;
    
    ApplicationContext context;

    public ManageNavigator(FolderType folderType, ApplicationContext context) {
        this.context = context;

        setSizeFull();
        setPadding(false);

        treeGrid = buildTreeGrid();
        addAndExpand(treeGrid);

        agentsFolder = new Folder();
        agentsFolder.setName("Agents");

        flowsFolder = new Folder();
        flowsFolder.setName("Flows");
    }

    public void refresh() {
        AbstractNamedObject selected = getCurrentSelection();
        List<AbstractNamedObject> expandedItems = new ArrayList<AbstractNamedObject>();
        List<AbstractNamedObject> items = getAllItems();
        for (AbstractNamedObject object : items) {
            if (treeGrid.isExpanded(object)) {
                expandedItems.add(object);
            }
        }

        treeGrid.getTreeData().clear();
                
        treeGrid.getTreeData().addItem(null, CURRENTLY_RUNNING);
        
        treeGrid.getTreeData().addItem(null, IN_ERROR);

        
        treeGrid.getTreeData().addItem(null, agentsFolder);

        List<Folder> folders = context.getConfigurationService().findFolders(null, FolderType.AGENT);
        for (Folder folder : folders) {
            addChildFolder(folder, agentsFolder);
        }
        
        addAgentsToFolder(null);

        treeGrid.getTreeData().addItem(null, flowsFolder);
        addFlowsToFolder(flowsFolder);
        treeGrid.getDataProvider().refreshAll();

        for (AbstractNamedObject object : expandedItems) {
            treeGrid.expand(object);
        }

        treeGrid.focus();
        if (getAllItems().contains(selected)) {
            treeGrid.select(selected);
        } else {
            treeGrid.select(CURRENTLY_RUNNING);
        }
    }
    
    protected List<AbstractNamedObject> getAllItems() {
        List<AbstractNamedObject> itemList = new ArrayList<AbstractNamedObject>();
        addItemsRecursively(null, itemList);
        return itemList;
    }
    
    protected void addItemsRecursively(AbstractNamedObject item, List<AbstractNamedObject> list) {
        if (item != null) {
            list.add(item);
        }
        for (AbstractNamedObject child : treeGrid.getTreeData().getChildren(item)) {
            addItemsRecursively(child, list);
        }
    }

    protected void addChildFolder(Folder folder, AbstractNamedObject root) {
        if (folder.getParent() != null) {
            treeGrid.getTreeData().addItem(folder.getParent(), folder);
        } else {
            treeGrid.getTreeData().addItem(root, folder);
        }
        treeGrid.getDataProvider().refreshAll();
        treeGrid.collapse(folder);

        List<Folder> children = folder.getChildren();
        for (Folder child : children) {
            addChildFolder(child, root);
        }
        if (folder.getFolderType() == FolderType.AGENT) {
            addAgentsToFolder(folder);
        }

    }

    @SuppressWarnings("unchecked")
    protected Set<Object> getGridValues() {
        Set<Object> selectedIds = null;
        Object obj = getCurrentSelection();
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

    protected TreeGrid<AbstractNamedObject> buildTreeGrid() {
        final TreeGrid<AbstractNamedObject> grid = new TreeGrid<AbstractNamedObject>();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        grid.setSizeFull();
        grid.setPageSize(100);
        grid.addComponentHierarchyColumn(item -> {
            Icon icon = null;
            String title = null;
            if (item.equals(CURRENTLY_RUNNING)) {
                icon = new Icon(VaadinIcon.COGS);
            } else if (item.equals(IN_ERROR)) {
                icon = new Icon(VaadinIcon.WARNING);
            } else if (item instanceof Folder) {
                icon = new Icon(grid.isExpanded(item) ? VaadinIcon.FOLDER_OPEN : VaadinIcon.FOLDER);
            } else if (item instanceof AgentName) {
                icon = new Icon(Icons.AGENT);
            } else if (item instanceof AgentDeploymentSummary) {
                icon = new Icon(Icons.DEPLOYMENT);
                title = ((AgentDeploymentSummary) item).getProjectName();
            } else if (item instanceof FlowName) {
                icon = new Icon(Icons.FLOW);
                if (item instanceof ProjectVersionFlowName) {
                    ProjectVersionFlowName flow = (ProjectVersionFlowName) item;
                    title = flow.projectVersion != null ? flow.projectVersion.getName() : "";
                }
            }
            if (icon != null) {
                icon.getStyle().set("min-width", "24px");
                HorizontalLayout layout = new HorizontalLayout(icon, new Span(item.getName()));
                if (title != null) {
                    layout.getElement().setProperty("title", title);
                }
                return layout;
            }
            return new Span(item.getName());
        }).setFlexGrow(1);

        grid.addItemClickListener((event) -> {
            if (event.getButton() == 0) {
                if (event.getClickCount() == 2) {
                    AbstractNamedObject item = event.getItem();
                    if (!treeGrid.getTreeData().getChildren(item).isEmpty()) {
                        if (treeGrid.isExpanded(item)) {
                            treeGrid.collapse(item);
                        } else {
                            treeGrid.expand(item);
                        }
                    }
                }
            }
        });

        grid.setClassNameGenerator(itemId -> {
            if (itemId instanceof Folder) {
                return "folder";
            } else {
                return null;
            }
        });

        return grid;
    }

    protected void addAgentsToFolder(Folder folder) {
        IOperationsService operationsService = context.getOperationsService();
        List<AgentName> agents = operationsService.findAgentsInFolder(folder);
        for (AgentName agent : agents) {

            List<AgentDeploymentSummary> deployments = operationsService.findAgentDeploymentSummary(agent.getId());

            treeGrid.getTreeData().addItem(folder != null ? folder : agentsFolder, agent);

            for (AgentDeploymentSummary agentDeployment : deployments) {
                if (agentDeployment.getType().equals(AgentDeploymentSummary.TYPE_FLOW)) {
                    treeGrid.getTreeData().addItem(agent, agentDeployment);
                }
            }
        }
        treeGrid.getDataProvider().refreshAll();
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
                treeGrid.getTreeData().addItem(folder, flow);
            }
        }
    }

    public void addValueChangeListener(SelectionListener<Grid<AbstractNamedObject>, AbstractNamedObject> listener) {
        treeGrid.addSelectionListener(listener);
    }

    public AbstractNamedObject getCurrentSelection() {
        return treeGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
    }

    public AbstractNamedObject getCurrentSelectionParent() {
        if (getCurrentSelection() != null) {
            return treeGrid.getTreeData().getParent(getCurrentSelection());
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
