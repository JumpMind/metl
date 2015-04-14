package org.jumpmind.symmetric.is.ui.views;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.EnableFocusTextField;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.is.ui.views.deploy.EditAgentPanel;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
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
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.MarginInfo;
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
import com.vaadin.ui.TextField;
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

    MenuItem newAgent;

    ApplicationContext context;

    TreeTable treeTable;

    AbstractObject lastSelected;

    AbstractObject itemBeingEdited;

    AbstractObject itemClicked;

    long itemClickTimeInMs;

    ShortcutListener treeTableEnterKeyShortcutListener;

    ShortcutListener treeTableDeleteKeyShortcutListener;

    TabbedPanel tabbedPanel;
    
    MenuItem search;

    HorizontalLayout searchBarLayout;

    public DeployNavigator(ApplicationContext context, TabbedPanel tabbedPanel) {

        this.context = context;
        this.tabbedPanel = tabbedPanel;
        setCaption("Navigator");
        setSizeFull();
        addStyleName("noborder");
        addStyleName(ValoTheme.MENU_ROOT);

        addComponent(buildMenuBar());
        
        searchBarLayout = buildSearchBar();
        addComponent(searchBarLayout);

        treeTable = buildTreeTable();
        treeTable.addStyleName("noselect");
        addComponent(treeTable);
        setExpandRatio(treeTable, 1);

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

        this.treeTable.removeAllItems();
        List<Folder> folders = context.getConfigurationService().findFolders(FolderType.RUNTIME);
        for (Folder folder : folders) {
            addChildFolder(folder);
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
    
    protected HorizontalLayout buildSearchBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(new MarginInfo(false, true, true, true));
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setVisible(false);
        TextField search = new TextField();
        search.setIcon(Icons.SEARCH);
        search.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        search.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(search);
        return layout;
    }

    protected HorizontalLayout buildMenuBar() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);

        MenuBar leftMenuBar = new MenuBar();
        leftMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenuBar.setWidth(100, Unit.PERCENTAGE);

        MenuItem newMenu = leftMenuBar.addItem("New", null);

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

        MenuBar rightMenuBar = new MenuBar();
        rightMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);

        search = rightMenuBar.addItem("", Icons.SEARCH, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                search.setChecked(!search.isChecked());
                searchBarLayout.setVisible(search.isChecked());
            }
        });
        
        delete = rightMenuBar.addItem("", Icons.DELETE, new Command() {

            @Override
            public void menuSelected(MenuItem selectedItem) {
                handleDelete();
            }
        });
        delete.setDescription("Remove");

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
        table.setContainerDataSource(new BeanItemContainer<AbstractObject>(AbstractObject.class));
        table.setTableFieldFactory(new DefaultFieldFactory() {
            @Override
            public Field<?> createField(Container container, Object itemId, Object propertyId,
                    Component uiContext) {
                return buildEditableNavigatorField(itemId);
            }
        });
        table.setVisibleColumns(new Object[] { "name" });
        table.setColumnExpandRatio("name", 1);
        treeTableDeleteKeyShortcutListener = new ShortcutListener("Delete", KeyCode.DELETE, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (delete.isEnabled()) {
                    handleDelete();
                }
            }
        };
        table.addShortcutListener(treeTableDeleteKeyShortcutListener);

        treeTableEnterKeyShortcutListener = new ShortcutListener("Enter", KeyCode.ENTER, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                openItem(getSelectedValue());
            }
        };
        table.addShortcutListener(treeTableEnterKeyShortcutListener);
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
                        itemClicked = null;

                        if (table.areChildrenAllowed(event.getItemId())) {
                            Object item = event.getItemId();
                            table.setCollapsed(item, !table.isCollapsed(item));
                        }
                    } else {
                        if (itemClicked != null && itemClicked.equals(event.getItemId())) {
                            long timeSinceClick = System.currentTimeMillis() - itemClickTimeInMs;
                            if (timeSinceClick > 600 && timeSinceClick < 2000) {
                                startEditingItem(itemClicked);
                            } else {
                                itemClicked = null;
                            }
                        } else if (event.getItemId() instanceof AbstractObject) {
                            itemClicked = (AbstractObject) event.getItemId();
                            itemClickTimeInMs = System.currentTimeMillis();
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
                    folderExpanded(folder);
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
            treeTable.removeShortcutListener(treeTableDeleteKeyShortcutListener);
            treeTable.removeShortcutListener(treeTableEnterKeyShortcutListener);
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
            treeTable.addShortcutListener(treeTableDeleteKeyShortcutListener);
            treeTable.addShortcutListener(treeTableEnterKeyShortcutListener);
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
            Object selected = itemBeingEdited;
            itemBeingEdited = null;
            itemClicked = null;
            refresh();
            treeTable.focus();
            treeTable.setValue(selected);
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
        if (item instanceof Agent) {
            Agent agent = (Agent)item;
            tabbedPanel.addCloseableTab(agent.getId(), agent.getName(), Icons.AGENT, new EditAgentPanel(context, tabbedPanel, agent));
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

    protected void folderExpanded(Folder folder) {
        List<Agent> agents = context.getConfigurationService().findAgentsInFolder(folder);
        for (Agent agent : agents) {
            addAgent(folder, agent);                       
        }
    }

    protected void selectionChanged(ValueChangeEvent event) {
        AbstractObject selected = getSelectedValue();
        Folder selectedFolder = getSelectedFolder();
        boolean showNewFolder = itemBeingEdited == null
                && (selected == null || selectedFolder != null);
        newFolder.setVisible(showNewFolder);
        newAgent.setVisible(selectedFolder != null);

        delete.setEnabled(isDeleteButtonEnabled(selected));
    }

    protected boolean isDeleteButtonEnabled(Object selected) {
        return selected instanceof Folder;
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
                                    context.getConfigurationService().deleteFolder(folder.getId());
                                } catch (Exception ex) {
                                    CommonUiUtils.notify(
                                            "Could not delete the \"" + folder.getName()
                                                    + "\" folder", Type.WARNING_MESSAGE);
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

    protected void addFolder() {
        Folder parentFolder = getSelectedFolder();

        Folder folder = new Folder();
        folder.setName("New Folder");
        folder.setType(FolderType.RUNTIME.name());
        folder.setParent(parentFolder);

        addChildFolder(folder);

        while (parentFolder != null) {
            treeTable.setCollapsed(parentFolder, false);
            parentFolder = parentFolder.getParent();
        }

        startEditingItem(folder);
    }

    protected void addAgent() {
        Folder folder = getSelectedFolder();
        if (folder != null) {
            Agent agent = new Agent();
            agent.setName("New Agent");
            agent.setFolder(folder);
            context.getConfigurationService().save(agent);

            addAgent(folder, agent);
        }

    }

    protected void addAgent(Folder folder, Agent agent) {
        treeTable.addItem(agent);
        treeTable.setItemIcon(agent, Icons.AGENT);
        treeTable.setParent(agent, folder);
        treeTable.setChildrenAllowed(agent, false);
    }
    
    protected void addChildFolder(Folder folder) {
        this.treeTable.addItem(folder);
        this.treeTable.setItemIcon(folder, FontAwesome.FOLDER);
        this.treeTable.setCollapsed(folder, true);
        if (folder.getParent() != null) {
            this.treeTable.setParent(folder, folder.getParent());
        }
        List<Folder> children = folder.getChildren();
        for (Folder child : children) {
            addChildFolder(child);
        }
    }

    protected void deleteTreeItems(AbstractObject obj) {

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
