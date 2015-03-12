package org.jumpmind.symmetric.is.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.common.Icons;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
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
        
        Agent rootAgent = new Agent();
        rootAgent.setName("Agents");
        treeTable.addItem(rootAgent);
        treeTable.setItemIcon(rootAgent, FontAwesome.GEAR);
        List<Folder> folders = configurationService.findFolders(FolderType.RUNTIME);
        for (Folder folder : folders) {
            addChildFolder(folder, rootAgent);
        }

        Flow rootFlow = new Flow();
        rootFlow.setName("Flows");
        treeTable.addItem(rootFlow);
        treeTable.setItemIcon(rootFlow, Icons.FLOW);
        folders = configurationService.findFolders(FolderType.DESIGN);
        for (Folder folder : folders) {
            addChildFolder(folder, rootFlow);
        }

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
    	addFlowsToFolder(folder);
    }

    protected void openItem(Object item) {
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

        table.addShortcutListener(new ShortcutListener("Enter", KeyCode.ENTER, null) {
            public void handleAction(Object sender, Object target) {
                Set<Object> selectedIds = getTableValues();
                for (Object object : selectedIds) {
                    openItem(object);
                }
            }
        });

        table.addItemClickListener(new ItemClickListener() {
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (event.isDoubleClick()) {

                    } else {

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
        List<Agent> agents = configurationService.findAgentsInFolder(folder);
        for (Agent agent : agents) {
            treeTable.addItem(agent);
            treeTable.setItemIcon(agent, FontAwesome.GEAR);
            treeTable.setChildrenAllowed(agent, agent.getAgentDeployments().size() > 0);
            treeTable.setParent(agent, folder);

            List<AgentDeployment> deployments = agent.getAgentDeployments();
            for (AgentDeployment agentDeployment : deployments) {
                treeTable.addItem(agentDeployment);
                treeTable.setItemIcon(agentDeployment, FontAwesome.CUBE);
                treeTable.setParent(agentDeployment, agent);
                treeTable.setChildrenAllowed(agentDeployment, false);
            }
        }

    }

    protected void addFlowsToFolder(Folder folder) {
        List<Flow> flows = configurationService.findFlowsInFolder(folder);
        for (Flow flow : flows) {
            treeTable.addItem(flow);
            treeTable.setItemIcon(flow, Icons.FLOW);
            treeTable.setParent(flow, folder);

            List<FlowVersion> versions = flow.getFlowVersions();
            for (FlowVersion flowVersion : versions) {
                treeTable.addItem(flowVersion);
                treeTable.setItemCaption(flowVersion, flowVersion.getVersionName());
                treeTable.setItemIcon(flowVersion, Icons.FLOW_VERSION);
                treeTable.setParent(flowVersion, flow);
                treeTable.setChildrenAllowed(flowVersion, false);
            }
        }
    }

    public void addValueChangeListener(ValueChangeListener listener) {
    	treeTable.addValueChangeListener(listener);
    }

    public Object getCurrentSelection() {
    	return treeTable.getValue();
    }

}
