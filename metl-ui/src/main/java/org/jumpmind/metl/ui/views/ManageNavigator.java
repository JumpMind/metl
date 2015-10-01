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
package org.jumpmind.metl.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.Icons;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class ManageNavigator extends Panel {

    IConfigurationService configurationService;

    TreeTable treeTable;

    Folder agentsFolder;

    Folder flowsFolder;

    public ManageNavigator(FolderType folderType, IConfigurationService configurationService) {
        this.configurationService = configurationService;

        setCaption("Navigator");
        setSizeFull();
        addStyleName("noborder");
        addStyleName(ValoTheme.MENU_ROOT);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

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
        treeTable.addItem(agentsFolder);
        treeTable.setItemIcon(agentsFolder, FontAwesome.FOLDER);
        addAgentsToFolder(agentsFolder);

        List<Folder> folders = configurationService.findFolders(null, FolderType.AGENT);
        for (Folder folder : folders) {
            addChildFolder(folder, agentsFolder);
        }

        treeTable.addItem(flowsFolder);
        treeTable.setItemIcon(flowsFolder, FontAwesome.FOLDER);

        for (Object object : expandedItems) {
            treeTable.setCollapsed(object, false);
        }

        treeTable.focus();
        if (treeTable.containsId(selected)) {
            treeTable.setValue(selected);
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
    }

    protected void folderExpanded(Folder folder) {
        addAgentsToFolder(folder);
        if (folder.getName().equals("Flows")) {
            addFlowsToFolder(folder);
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
        table.setContainerDataSource(new BeanItemContainer<AbstractObject>(AbstractObject.class));
        table.setVisibleColumns(new Object[] { "name" });
        table.setColumnExpandRatio("name", 1);

        table.addItemClickListener(new ItemClickListener() {
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (event.isDoubleClick()) {
                        if (treeTable.hasChildren(event.getItemId())) {
                            treeTable.setCollapsed(event.getItemId(),
                                    !treeTable.isCollapsed(event.getItemId()));
                        }
                    }
                }
            }
        });

        table.addCollapseListener(new CollapseListener() {
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof Folder) {
                    table.setItemIcon(event.getItemId(), FontAwesome.FOLDER);
                }
            }
        });

        table.addExpandListener(new ExpandListener() {
            public void nodeExpand(ExpandEvent event) {
                if (event.getItemId() instanceof Folder) {
                    Folder folder = (Folder) event.getItemId();
                    table.setItemIcon(folder, FontAwesome.FOLDER_OPEN);
                    folderExpanded(folder);
                }
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

        return table;
    }

    protected void addAgentsToFolder(Folder folder) {
        List<Agent> agents = configurationService.findAgentsInFolder(folder == agentsFolder ? null
                : folder);
        for (Agent agent : agents) {
            treeTable.addItem(agent);
            treeTable.setItemIcon(agent, Icons.AGENT);
            treeTable.setChildrenAllowed(agent, agent.getAgentDeployments().size() > 0);
            treeTable.setParent(agent, folder);

            List<AgentDeployment> deployments = agent.getAgentDeployments();
            for (AgentDeployment agentDeployment : deployments) {
                treeTable.addItem(agentDeployment);
                treeTable.setItemIcon(agentDeployment, Icons.DEPLOYMENT);
                treeTable.setParent(agentDeployment, agent);
                treeTable.setChildrenAllowed(agentDeployment, false);
            }
        }

    }

    protected void addFlowsToFolder(Folder folder) {
        List<FlowName> flows = configurationService.findFlows();
        for (FlowName flow : flows) {
            treeTable.addItem(flow);
            treeTable.setItemIcon(flow, Icons.FLOW);
            treeTable.setParent(flow, folder);
            treeTable.setChildrenAllowed(flow, false);
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

}
