package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.support.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.is.ui.support.PromptDialog.IPromptListener;
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
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

abstract public class AbstractFolderNavigatorLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected IConfigurationService configurationService;

    protected TreeTable tree;

    protected MenuItem addButton;

    protected Button delButton;

    protected FolderType folderType;

    protected Set<Object> lastSelected;

    public AbstractFolderNavigatorLayout(String title, FolderType folderType) {
        this.folderType = folderType;

        setMargin(new MarginInfo(false, true, true, true));
        setSpacing(true);
        setSizeFull();

        Label titleLabel = new Label(title);
        titleLabel.addStyleName("h2");

        this.tree = buildTree();

        addComponent(titleLabel);
        addComponent(buildButtonLayout());
        addComponent(tree);
        setExpandRatio(tree, 1);
    }

    protected TreeTable buildTree() {
        final TreeTable tree = new TreeTable();
        tree.setSizeFull();
        tree.setImmediate(true);
        tree.setMultiSelect(true);
        tree.setSelectable(true);
        tree.addGeneratedColumn("Name", new ColumnGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                Label label = new Label(itemId.toString());
                label.addStyleName("leftPad");
                return label;
            }
        });
        tree.addShortcutListener(new ShortcutListener("Delete", KeyCode.DELETE, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (delButton.isEnabled()) {
                    @SuppressWarnings("unchecked")
                    Set<Object> selectedIds = (Set<Object>) tree.getValue();
                    for (Object object : selectedIds) {
                        deleteTreeItem(object);
                    }
                }
            }
        });

        tree.addShortcutListener(new ShortcutListener("Enter", KeyCode.ENTER, null) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                @SuppressWarnings("unchecked")
                Set<Object> selectedIds = (Set<Object>) tree.getValue();
                for (Object object : selectedIds) {
                    itemClicked(object);
                }
            }
        });
        tree.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public void valueChange(ValueChangeEvent event) {
                lastSelected = (Set<Object>) tree.getValue();
                treeSelectionChanged(event);
            }
        });
        tree.addItemClickListener(new ItemClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                if (lastSelected != null && lastSelected.contains(event.getItemId())) {
                    log.info("unselected " + event.getItemId());
                    tree.unselect(event.getItemId());
                }
                if (event.isDoubleClick()) {
                    itemClicked(event.getItemId());
                }
            }
        });
        tree.addCollapseListener(new CollapseListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof Folder) {
                    tree.setItemIcon(event.getItemId(), FontAwesome.FOLDER);
                }
            }
        });
        tree.addExpandListener(new ExpandListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void nodeExpand(ExpandEvent event) {
                if (event.getItemId() instanceof Folder) {
                    Folder folder = (Folder) event.getItemId();
                    tree.setItemIcon(folder, FontAwesome.FOLDER_OPEN);
                    folderExpanded(folder);
                }
            }
        });
        return tree;
    }

    protected void focusAndSelectFirstItem() {
        tree.focus();
        Collection<?> allItems = tree.getItemIds();
        if (allItems.size() > 0) {
            tree.select(allItems.iterator().next());
        }
    }

    protected void expand(Folder folder, Object itemToSelect) {
        List<Folder> toExpand = new ArrayList<Folder>();
        toExpand.add(0, folder);
        while (folder != null) {
            folder = folder.getParent();
            if (folder != null) {
                toExpand.add(0, folder);
            }
        }

        for (Folder expandMe : toExpand) {
            tree.setCollapsed(expandMe, false);
        }

        tree.focus();
        tree.select(itemToSelect);

    }

    protected void itemClicked(Object item) {
    }

    protected HorizontalLayout buildButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        addComponent(buttonLayout);

        MenuBar bar = buildAddButton();
        buttonLayout.addComponent(bar);
        buttonLayout.setComponentAlignment(bar, Alignment.MIDDLE_LEFT);

        addButtonsAfterAdd(buttonLayout);

        delButton = createButton("Delete", false, new DeleteButtonClickListener());
        buttonLayout.addComponent(delButton);
        buttonLayout.setComponentAlignment(delButton, Alignment.MIDDLE_LEFT);

        addButtonsRight(buttonLayout);
        return buttonLayout;
    }

    protected Button createButton(String name, boolean enabled, ClickListener listener) {
        Button button = new Button(name);
        button.setStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        button.setEnabled(enabled);
        button.addClickListener(listener);
        return button;
    }

    protected void removeAllNonFolderChildren(Folder folder) {
        Collection<?> children = tree.getChildren(folder);
        if (children != null) {
            children = new HashSet<Object>(children);
            for (Object child : children) {
                if (!(child instanceof Folder)) {
                    tree.removeItem(child);
                }
            }
        }
    }

    protected Folder getSelectedFolder() {
        @SuppressWarnings("unchecked")
        Set<Object> selectedIds = (Set<Object>) tree.getValue();
        for (Object object : selectedIds) {
            if (object instanceof Folder) {
                return (Folder) object;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getSingleSelection(Class<T> clazz) {
        Set<Object> selectedIds = (Set<Object>) tree.getValue();
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
        Set<Object> selected = (Set<Object>) tree.getValue();
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

    private MenuBar buildAddButton() {
        MenuBar bar = new MenuBar();
        bar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
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
        return bar;
    }

    protected void addToAddButton(MenuBar.MenuItem dropdown) {
    }

    protected void addButtonsAfterAdd(HorizontalLayout buttonLayout) {
    }

    protected void addButtonsRight(HorizontalLayout buttonLayout) {
    }

    protected void deleteSelectedFolders() {
        @SuppressWarnings("unchecked")
        Set<Folder> folders = (Set<Folder>) tree.getValue();
        for (Folder folder : folders) {
            try {
                configurationService.deleteFolder(folder.getData().getId());
            } catch (Exception ex) {
                Notification.show("Could not delete the \"" + folder.getData().getName()
                        + "\" folder", Type.WARNING_MESSAGE);
            }
        }
    }

    public void refresh() {
        @SuppressWarnings({ "unchecked" })
        Set<Object> selected = (Set<Object>) tree.getValue();
        List<Object> expandedItems = new ArrayList<Object>();
        Collection<?> items = tree.getItemIds();
        for (Object object : items) {
            if (!tree.isCollapsed(object)) {
                expandedItems.add(object);
            }
        }

        this.tree.removeAllItems();
        List<Folder> folders = configurationService.findFolders(folderType);
        for (Folder folder : folders) {
            addChildFolder(folder);
        }

        for (Object object : expandedItems) {
            tree.setCollapsed(object, false);
        }

        tree.focus();
        tree.setValue(selected);

    }

    protected void addChildFolder(Folder folder) {
        this.tree.addItem(folder);
        this.tree.setItemIcon(folder, FontAwesome.FOLDER);
        this.tree.setCollapsed(folder, true);
        if (folder.getParent() != null) {
            this.tree.setParent(folder, folder.getParent());
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
                Set<Folder> selectedIds = (Set<Folder>) tree.getValue();
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
                    tree.setCollapsed(parentFolder, false);
                    parentFolder = parentFolder.getParent();
                }

                Set<Folder> selected = new HashSet<Folder>();
                selected.add(folder);
                tree.setValue(selected);
                return true;
            } else {
                return false;
            }
        }
    }

    class DeleteButtonClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            Button button = event.getButton();
            if (button == delButton) {
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
                Set<Object> objects = (Set<Object>) tree.getValue();
                for (Object object : objects) {
                    if (!(object instanceof Folder)) {
                        deleteTreeItem(object);
                    }
                }
            }
        }
    }

}
