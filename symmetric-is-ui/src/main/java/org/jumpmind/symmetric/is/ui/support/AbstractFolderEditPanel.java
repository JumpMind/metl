package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.support.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.is.ui.support.PromptDialog.IPromptListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
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
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

abstract public class AbstractFolderEditPanel extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    protected Tree tree;

    protected MenuItem addButton;

    protected Button delButton;

    @Autowired
    protected IConfigurationService configurationService;

    protected FolderType folderType;

    //protected Object lastSelected;

    public AbstractFolderEditPanel(String title, FolderType folderType) {
        this.folderType = folderType;

        setMargin(new MarginInfo(false, true, true, true));
        setSpacing(true);
        setSizeFull();

        Label titleLabel = new Label(title);
        titleLabel.addStyleName("h2");
        addComponent(titleLabel);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        addComponent(buttonLayout);

        MenuBar bar = buildAddButton();
        buttonLayout.addComponent(bar);
        buttonLayout.setComponentAlignment(bar, Alignment.MIDDLE_LEFT);

        delButton = new Button("Delete");
        delButton.setStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        delButton.setEnabled(false);
        delButton.addClickListener(new DeleteButtonClickListener());
        buttonLayout.addComponent(delButton);
        buttonLayout.setComponentAlignment(delButton, Alignment.MIDDLE_LEFT);

        addButtonsRight(buttonLayout);

        this.tree = new Tree();
        this.tree.setImmediate(true);
        this.tree.setMultiSelect(true);
        this.tree.setSelectable(true);
        this.tree.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                treeSelectionChanged(event);
            }
        });
        this.tree.addItemClickListener(new ItemClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                unselectIfSelected();
            }
        });
        this.tree.addCollapseListener(new CollapseListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void nodeCollapse(CollapseEvent event) {
                if (event.getItemId() instanceof Folder) {
                    tree.setItemIcon(event.getItemId(), FontAwesome.FOLDER);
                }
            }
        });
        this.tree.addExpandListener(new ExpandListener() {

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

        addComponent(tree);
        setExpandRatio(tree, 1);
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

    protected void folderExpanded(Folder folder) {

    }

    protected void unselectIfSelected() {
//        @SuppressWarnings("unchecked")
//        Set<Object> selected = (Set<Object>) tree.getValue();
//        if (selected.size() > 0) {
//            Object selectedObject = selected.iterator().next();
//            if (selected.contains(lastSelected)) {
//                tree.unselect(lastSelected);
//                lastSelected = null;                
//            } else {
//                lastSelected = selectedObject;
//            }
//        }
    }

    protected void treeSelectionChanged(ValueChangeEvent event) {
        @SuppressWarnings("unchecked")
        Set<Object> selected = (Set<Object>) tree.getValue();
        Folder folder = getSelectedFolder();
        addButton.setEnabled((folder == null && selected.size() == 0) || folder != null);
        delButton.setEnabled(selected.size() > 0);
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
        this.tree.removeAllItems();
        List<Folder> folders = configurationService.findFolders(folderType);
        for (Folder folder : folders) {
            addChildFolder(folder);
        }
    }

    protected void addChildFolder(Folder folder) {
        this.tree.addItem(folder);
        this.tree.setItemCaption(folder, folder.getData().getName());
        this.tree.setItemIcon(folder, FontAwesome.FOLDER);
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
                    tree.expandItem(parentFolder);
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
