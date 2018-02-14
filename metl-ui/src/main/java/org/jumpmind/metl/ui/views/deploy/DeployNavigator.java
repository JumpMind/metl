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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentName;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.util.AppConstants;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.EnableFocusTextField;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.ImportDialog;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.ImportDialog.IImportListener;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.ConfirmDialog.IConfirmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification.Type;
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
public class DeployNavigator extends VerticalLayout {

    final Logger log = LoggerFactory.getLogger(getClass());

    MenuItem newFolder;

    MenuItem delete;
    
    MenuItem rename;
    
    MenuItem open;

    MenuItem newAgent;

    MenuItem miImport;
    
    MenuItem export;    
    
    ApplicationContext context;

    TreeTable treeTable;

    AbstractObject lastSelected;

    AbstractObject itemBeingEdited;

    TabbedPanel tabbedPanel;

    public DeployNavigator(ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;
        setCaption("Navigator");
        setSizeFull();
        addStyleName("noborder");
        addStyleName(ValoTheme.MENU_ROOT);

        addComponent(buildMenuBar());

        treeTable = buildTreeTable();
        treeTable.addStyleName("noselect");
        addComponent(treeTable);
        setExpandRatio(treeTable, 1);
        selectionChanged(null);

    }

    public void select(Object obj) {
        Object parent = obj;
        do {
            parent = treeTable.getParent(parent);
            if (parent != null) {
                treeTable.setCollapsed(parent, false);
            }
        } while (parent != null);

        treeTable.setValue(obj);
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
        List<Folder> folders = context.getConfigurationService().findFolders(null,
                FolderType.AGENT);
        for (Folder folder : folders) {
            addChildren(folder);
        }
        
        List<AgentName> agents = context.getOperationsService().findAgentsInFolder(null);
        for (AgentName agent : agents) {
            addAgent(null, agent);
        }

        for (Object object : expandedItems) {
            treeTable.setCollapsed(object, false);
        }

        treeTable.focus();
        if (treeTable.containsId(selected)) {
            treeTable.setValue(selected);
        } else {
            if (treeTable.getItemIds().size() > 0) {
                treeTable.setValue(treeTable.getItemIds().iterator().next());
            }
        }

        treeTable.focus();
    }

    protected HorizontalLayout buildMenuBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);

        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);

        MenuItem fileMenu = leftMenuBar.addItem("File", null);
        
        MenuItem newMenu = fileMenu.addItem("New", null);
        
        miImport = fileMenu.addItem("Import",new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                importAgentData();
            }
        });
        
        export = fileMenu.addItem("Export", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                exportAgentData();
            }
        });

        newFolder = newMenu.addItem("Folder", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addFolder();
            }
        });

        newAgent = newMenu.addItem("Agent", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                addAgent();
            }
        });

        MenuItem editMenu = leftMenuBar.addItem("Edit", null);

        open = editMenu.addItem("Open", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                openItem(treeTable.getValue());
            }
        });

        rename = editMenu.addItem("Rename", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                startEditingItem((AbstractObject) treeTable.getValue());
            }
        });

        delete = editMenu.addItem("Remove", new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                handleDelete();
            }
        });

        MenuBar rightMenuBar = new MenuBar();
        rightMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);

        layout.addComponent(leftMenuBar);
        layout.addComponent(rightMenuBar);
        layout.setExpandRatio(leftMenuBar, 1);

        return layout;
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
        table.setEditable(true);
        table.setContainerDataSource(new BeanItemContainer<AbstractNamedObject>(AbstractNamedObject.class));
        table.setTableFieldFactory(new DefaultFieldFactory() {
            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId,
                    Component uiContext) {
                return buildEditableNavigatorField(itemId);
            }
        });
        table.setVisibleColumns(new Object[] { "name" });
        table.setColumnExpandRatio("name", 1);
        table.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                lastSelected = getSelectedValue();
                selectionChanged(event);
            }
        });
        table.addItemClickListener(new ItemClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (event.isDoubleClick()) {
                        abortEditingItem();
                        openItem(event.getItemId());
                        if (table.areChildrenAllowed(event.getItemId())) {
                            Object item = event.getItemId();
                            table.setCollapsed(item, !table.isCollapsed(item));
                        }
                    }
                }
            }
        });
        table.addCollapseListener(new CollapseListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof Folder) {
                    table.setItemIcon(event.getItemId(), FontAwesome.FOLDER);
                }
            }
        });
        table.addExpandListener(new ExpandListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void nodeExpand(ExpandEvent event) {
                if (event.getItemId() instanceof Folder) {
                    Folder folder = (Folder) event.getItemId();
                    table.setItemIcon(folder, FontAwesome.FOLDER_OPEN);
                }
            }
        });
        table.setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
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

    protected boolean startEditingItem(AbstractObject obj) {
        if (obj.isSettingNameAllowed()) {
            itemBeingEdited = obj;
            treeTable.refreshRowCache();
            return true;
        } else {
            return false;
        }

    }

    protected void finishEditingItem() {
        if (itemBeingEdited != null) {
            IConfigurationService configurationService = context.getConfigurationService();
            Object selected = itemBeingEdited;
            Method method = null;
            try {
                method = configurationService.getClass().getMethod("save",
                        itemBeingEdited.getClass());
            } catch (NoSuchMethodException e) {
            } catch (SecurityException e) {
            }
            if (method != null) {
                try {
                    method.invoke(configurationService, itemBeingEdited);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                configurationService.save(itemBeingEdited);
            }
            itemBeingEdited = null;
            treeTable.refreshRowCache();
            treeTable.focus();
            treeTable.setValue(selected);
        }
    }

    protected void abortEditingItem() {
        if (itemBeingEdited != null) {
            itemBeingEdited = null;
            refresh();
            treeTable.focus();
        }
    }

    protected Field<?> buildEditableNavigatorField(Object itemId) {
        if (itemBeingEdited != null && itemBeingEdited.equals(itemId)) {
            final EnableFocusTextField field = new EnableFocusTextField();
            field.addStyleName(ValoTheme.TEXTFIELD_SMALL);
            field.setImmediate(true);
            field.addFocusListener(new FocusListener() {

                @Override
                public void focus(FocusEvent event) {
                    field.setFocusAllowed(false);
                    field.selectAll();
                    field.setFocusAllowed(true);
                }
            });
            field.focus();
            field.addShortcutListener(new ShortcutListener("", KeyCode.ESCAPE, null) {

                @Override
                public void handleAction(Object sender, Object target) {
                    abortEditingItem();
                }
            });
            field.addShortcutListener(new ShortcutListener("Enter", KeyCode.ENTER, null) {

                private static final long serialVersionUID = 1L;

                @Override
                public void handleAction(Object sender, Object target) {
                    finishEditingItem();
                }
            });
            field.addBlurListener(new BlurListener() {
                @Override
                public void blur(BlurEvent event) {
                    finishEditingItem();
                }
            });
            return field;
        } else {
            return null;
        }
    }

    protected AbstractObject getSelectedValue() {
        return (AbstractObject) treeTable.getValue();
    }

    protected void openItem(Object item) {
        if (item instanceof AgentName) {
            AgentName agentName = (AgentName) item;
            Agent agent = context.getOperationsService().findAgent(agentName.getId(), true);
            tabbedPanel.addCloseableTab(agent.getId(), agent.getName(), Icons.AGENT,
                    new EditAgentPanel(context, tabbedPanel, agent));
        }
    }

    protected Folder getSelectedFolder() {
        Object object = getSelectedValue();
        if (object instanceof Folder) {
            return (Folder) object;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getSingleSelection(Class<T> clazz) {
        Object obj = getSelectedValue();
        if (obj != null && clazz.isAssignableFrom(obj.getClass())) {
            return (T) obj;
        }
        return null;
    }

    protected void selectionChanged(ValueChangeEvent event) {
        AbstractObject selected = getSelectedValue();
        Folder selectedFolder = getSelectedFolder();
        boolean showNewFolder = itemBeingEdited == null
                && (selected == null || selectedFolder != null);
        newFolder.setEnabled(showNewFolder);
        newAgent.setEnabled(selectedFolder == null || !selectedFolder.getName().startsWith("<"));

        delete.setEnabled(isDeleteButtonEnabled(selected));
        rename.setEnabled(isDeleteButtonEnabled(selected));
        open.setEnabled(selected != null && selectedFolder == null);
    }

    protected boolean isDeleteButtonEnabled(Object selected) {
        Folder selectedFolder = getSelectedFolder();
        return (selectedFolder != null && !selectedFolder.getName().startsWith("<"))
                || selected instanceof AgentName;
    }

    protected void handleDelete() {
        if (getSelectedFolder() != null) {
            ConfirmDialog.show("Delete Folder?",
                    "Are you sure you want to delete the selected folders?",
                    new IConfirmListener() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean onOk() {
                            Object obj = getSelectedValue();
                            if (obj instanceof Folder) {
                                Folder folder = (Folder) obj;
                                try {
                                    context.getConfigurationService().delete(folder);
                                } catch (Exception ex) {
                                    log.error("", ex);
                                    CommonUiUtils.notify("Could not delete the \""
                                            + folder.getName() + "\" folder", Type.WARNING_MESSAGE);
                                }
                            }
                            refresh();
                            return true;
                        }
                    });
        }

        AbstractObject obj = getSelectedValue();
        if (!(obj instanceof Folder)) {
            deleteTreeItems(obj);
        }
    }

    protected void importAgentData() {
        ImportDialog.show("Import Config", "Click the upload button to import your config", new ImportConfigurationListener());
    }
    
    class ImportConfigurationListener implements IImportListener {
        @Override
        public void onFinished(String dataToImport) {
            context.getImportExportService().importConfiguration(dataToImport, context.getUser().getLoginId());
            context.getDefinitionFactory().refresh();
            refresh();
        }
    }

    protected void exportAgentData() {
        AgentName agent = (AgentName) treeTable.getValue();
        final String export = context.getImportExportService().exportAgent(agent.getId(), AppConstants.SYSTEM_USER);
        StreamSource ss = new StreamSource() {
            private static final long serialVersionUID = 1L;

            public InputStream getStream() {
                try {
                    return new ByteArrayInputStream(export.getBytes());
                } catch (Exception e) {
                    log.error("Failed to export configuration", e);
                    CommonUiUtils.notify("Failed to export configuration.", Type.ERROR_MESSAGE);
                    return null;
                }

            }
        };
        String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        StreamResource resource = new StreamResource(ss,
                String.format("%s-config-%s.json", agent.getName().toLowerCase().replaceAll(" ", "-"), datetime));
        final String KEY = "export";
        setResource(KEY, resource);
        Page.getCurrent().open(ResourceReference.create(resource, this, KEY).getURL(), null);
        
    }
    
    protected void addFolder() {
        Folder parentFolder = getSelectedFolder();

        Folder folder = new Folder();
        folder.setName("New Folder");
        folder.setType(FolderType.AGENT.name());
        folder.setParent(parentFolder);

        addChildren(folder);

        while (parentFolder != null) {
            treeTable.setCollapsed(parentFolder, false);
            parentFolder = parentFolder.getParent();
        }

        startEditingItem(folder);
    }

    protected void addAgent() {
        Folder folder = getSelectedFolder();
        Agent agent = new Agent();
        agent.setName("New Agent");
        agent.setFolder(folder);
        context.getConfigurationService().save(agent);
        context.getAgentManager().refresh(agent);
        AgentName name = new AgentName(agent);
        addAgent(folder, name);
        expand(folder, name);
        startEditingItem(name);
    }

    protected void addAgent(Folder folder, AgentName agent) {
        treeTable.setChildrenAllowed(folder, true);
        treeTable.addItem(agent);
        treeTable.setItemIcon(agent, Icons.AGENT);
        treeTable.setParent(agent, folder);
        treeTable.setChildrenAllowed(agent, false);
    }

    protected void addChildren(Folder folder) {
        if (folder.getParent() != null) {
            treeTable.setChildrenAllowed(folder.getParent(), true);
        }
        treeTable.addItem(folder);
        treeTable.setItemIcon(folder, FontAwesome.FOLDER);
        treeTable.setCollapsed(folder, true);
        treeTable.setChildrenAllowed(folder, false);
        if (folder.getParent() != null) {
            treeTable.setParent(folder, folder.getParent());
        }

        List<Folder> children = folder.getChildren();
        for (Folder child : children) {
            addChildren(child);
        }

        List<AgentName> agents = context.getOperationsService().findAgentsInFolder(folder);
        for (AgentName agent : agents) {
            addAgent(folder, agent);
        }

    }

    protected void deleteTreeItems(AbstractObject obj) {
        if (obj instanceof AgentName) {
            AgentName agentName = (AgentName) obj;
            ConfirmDialog.show("Delete Agent?",
                    "Are you sure you want to delete the '" + agentName.getName() + "' agent?",
                    () -> {
                        Agent agent = context.getOperationsService().findAgent(agentName.getId(),
                                false);
                        context.getConfigurationService().delete(agent);
                        context.getAgentManager().refresh(agent);
                        refresh();
                        return true;
                    });
        }
    }

    protected void removeAllNonFolderChildren(Folder folder) {
        Collection<?> children = treeTable.getChildren(folder);
        if (children != null) {
            children = new HashSet<Object>(children);
            for (Object child : children) {
                if (!(child instanceof Folder)) {
                    treeTable.removeItem(child);
                }
            }
        }
    }

    protected void expand(Folder folder, Object itemToSelect) {
        List<Folder> toExpand = new ArrayList<Folder>();
        toExpand.add(0, folder);
        treeTable.unselect(folder);
        while (folder != null) {
            folder = folder.getParent();
            if (folder != null) {
                toExpand.add(0, folder);
            }
        }

        for (Folder expandMe : toExpand) {
            treeTable.setCollapsed(expandMe, false);
        }

        treeTable.focus();
        treeTable.select(itemToSelect);
    }

}
