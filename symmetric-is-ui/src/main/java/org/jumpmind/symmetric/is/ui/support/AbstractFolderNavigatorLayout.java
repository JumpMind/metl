package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.ui.common.PromptDialog;
import org.jumpmind.symmetric.ui.common.PromptDialog.IPromptListener;
import org.jumpmind.symmetric.ui.common.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

abstract public class AbstractFolderNavigatorLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected IConfigurationService configurationService;

    protected TreeTable treeTable;

    protected MenuItem addButton;

    protected MenuItem delButton;

    protected MenuItem editButton;

    protected FolderType folderType;

    protected Set<Object> lastSelected;

    public AbstractFolderNavigatorLayout(String title, FolderType folderType) {
        this.folderType = folderType;

        setMargin(new MarginInfo(false, true, true, true));
        setSpacing(true);
        setSizeFull();

        Label titleLabel = new Label(title);
        titleLabel.addStyleName("h2");

        this.treeTable = buildTree();

        addComponent(titleLabel);
        addComponent(buildButtonLayout());
        addComponent(treeTable);
        setExpandRatio(treeTable, 1);
    }

    protected TreeTable buildTree() {
        final TreeTable table = new TreeTable();
        table.setSizeFull();
        table.setImmediate(true);
        table.setMultiSelect(true);
        table.setSelectable(true);
        table.addGeneratedColumn("Name", new ColumnGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                Label label = new Label(itemId.toString());
                label.addStyleName("leftPad");
                return label;
            }
        });
        table.setColumnWidth("Name", 175);
        table.addShortcutListener(new ShortcutListener("Delete", KeyCode.DELETE, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (delButton.isEnabled()) {
                    @SuppressWarnings("unchecked")
                    Set<Object> selectedIds = (Set<Object>) table.getValue();
                    for (Object object : selectedIds) {
                        deleteTreeItem(object);
                    }
                }
            }
        });

        table.addShortcutListener(new ShortcutListener("Enter", KeyCode.ENTER, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                @SuppressWarnings("unchecked")
                Set<Object> selectedIds = (Set<Object>) table.getValue();
                for (Object object : selectedIds) {
                    itemClicked(object);
                }
            }
        });
        table.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;
            @SuppressWarnings("unchecked")
            @Override
            public void valueChange(ValueChangeEvent event) {
                lastSelected = (Set<Object>) table.getValue();
                treeSelectionChanged(event);
            }
        });
        table.addItemClickListener(new ItemClickListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.getButton() == MouseButton.LEFT) {
                    if (lastSelected != null && lastSelected.contains(event.getItemId())) {
                        table.unselect(event.getItemId());
                    }
                    if (event.isDoubleClick()) {
                        itemClicked(event.getItemId());
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
                if (itemId instanceof Folder && "Name".equals(propertyId)) {
                    return "folder";
                } else {
                    return null;    
                }
                
            }
        });
        return table;
    }

    protected void focusAndSelectFirstItem() {
        treeTable.focus();
        Collection<?> allItems = treeTable.getItemIds();
        if (allItems.size() > 0) {
            treeTable.select(allItems.iterator().next());
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

    protected void itemClicked(Object item) {
    }

    protected MenuBar buildButtonLayout() {
        MenuBar menuBar = new MenuBar();
        addComponent(menuBar);
        buildAddButton(menuBar);
        editButton = createButton(menuBar, "Edit", false, new EditButtonClickListener());
        delButton = createButton(menuBar, "Delete", false, new DeleteButtonClickListener());
        addButtonsRight(menuBar);
        return menuBar;
    }

    protected MenuItem createButton(MenuBar menuBar, String name, boolean enabled, Command listener) {
        MenuItem item = menuBar.addItem(name, listener);
        item.setEnabled(enabled);
        return item;
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

    protected Folder getSelectedFolder() {
        @SuppressWarnings("unchecked")
        Set<Object> selectedIds = (Set<Object>) treeTable.getValue();
        for (Object object : selectedIds) {
            if (object instanceof Folder) {
                return (Folder) object;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getSingleSelection(Class<T> clazz) {
        Set<Object> selectedIds = (Set<Object>) treeTable.getValue();
        if (selectedIds != null && selectedIds.size() == 1) {
            Object obj = selectedIds.iterator().next();
            if (obj != null && clazz.isAssignableFrom(obj.getClass())) {
                return (T) obj;
            }
        }
        return null;
    }

    protected void folderExpanded(Folder folder) {

    }

    protected void treeSelectionChanged(ValueChangeEvent event) {
        @SuppressWarnings("unchecked")
        Set<Object> selected = (Set<Object>) treeTable.getValue();
        Folder folder = getSelectedFolder();
        addButton.setEnabled((folder == null && selected.size() == 0) || folder != null);

        boolean deleteEnabled = false;
        for (Object object : selected) {
            deleteEnabled |= isDeleteButtonEnabled(object);
        }
        delButton.setEnabled(deleteEnabled);
    }

    protected boolean isDeleteButtonEnabled(Object selected) {
        return selected instanceof Folder;
    }

    private void buildAddButton(MenuBar bar) {
        MenuBar.MenuItem dropdown = bar.addItem("Add", null);
        addButton = dropdown.addItem("Folder", FontAwesome.FOLDER, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                PromptDialog.prompt("Add Folder", "Please choose a folder name",
                        new NewFolderPromptListener());
            }
        });
        addToAddButton(dropdown);
    }

    protected void addToAddButton(MenuItem dropdown) {
    }

    protected void addButtonsRight(MenuBar menuBar) {
    }

    protected void deleteSelectedFolders() {
        @SuppressWarnings("unchecked")
        Set<Folder> folders = (Set<Folder>) treeTable.getValue();
        for (Folder folder : folders) {
            try {
                configurationService.deleteFolder(folder.getData().getId());
            } catch (Exception ex) {
                UiUtils.notify("Could not delete the \"" + folder.getData().getName()
                        + "\" folder", Type.WARNING_MESSAGE);
            }
        }
    }

    public void refresh() {
        @SuppressWarnings({ "unchecked" })
        Set<Object> selected = (Set<Object>) treeTable.getValue();
        List<Object> expandedItems = new ArrayList<Object>();
        Collection<?> items = treeTable.getItemIds();
        for (Object object : items) {
            if (!treeTable.isCollapsed(object)) {
                expandedItems.add(object);
            }
        }

        this.treeTable.removeAllItems();
        List<Folder> folders = configurationService.findFolders(folderType);
        for (Folder folder : folders) {
            addChildFolder(folder);
        }

        for (Object object : expandedItems) {
            treeTable.setCollapsed(object, false);
        }

        treeTable.focus();
        treeTable.setValue(selected);

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

    protected void deleteTreeItem(Object object) {

    }

    class NewFolderPromptListener implements IPromptListener {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean onOk(String content) {
            if (isNotBlank(content)) {
                Folder parentFolder = null;
                @SuppressWarnings("unchecked")
                Set<Folder> selectedIds = (Set<Folder>) treeTable.getValue();
                if (selectedIds != null && selectedIds.size() > 0) {
                    parentFolder = selectedIds.iterator().next();
                }
                FolderData folderData = new FolderData();
                folderData.setName(content);
                folderData.setType(folderType.name());
                folderData.setParentFolderId(parentFolder != null ? parentFolder.getData().getId()
                        : null);
                Folder folder = new Folder(folderData);
                folder.setParent(parentFolder);

                configurationService.save(new Folder(folderData));

                refresh();

                while (parentFolder != null) {
                    treeTable.setCollapsed(parentFolder, false);
                    parentFolder = parentFolder.getParent();
                }

                Set<Folder> selected = new HashSet<Folder>();
                selected.add(folder);
                treeTable.setValue(selected);
                return true;
            } else {
                return false;
            }
        }
    }

    class DeleteButtonClickListener implements Command {

        private static final long serialVersionUID = 1L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            if (getSelectedFolder() != null) {
                ConfirmDialog.show("Delete Folder?",
                        "Are you sure you want to delete the selected folders?",
                        new IConfirmListener() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public boolean onOk() {
                                deleteSelectedFolders();
                                refresh();
                                return true;
                            }
                        });
            }

            @SuppressWarnings("unchecked")
            Set<Object> objects = (Set<Object>) treeTable.getValue();
            for (Object object : objects) {
                if (!(object instanceof Folder)) {
                    deleteTreeItem(object);
                }
            }
        }
    }

    class EditButtonClickListener implements Command {

        private static final long serialVersionUID = 1L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            Object item = getSingleSelection(Object.class);
            if (item != null) {
                itemClicked(item);
            }
        }
    }

}
