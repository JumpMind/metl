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
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.CollapseListener;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

abstract public class AbstractFolderEditPanel extends VerticalLayout implements ClickListener,
        IPromptListener {

    private static final long serialVersionUID = 1L;

    protected Tree tree;

    protected Button addButton;

    protected Button delButton;

    @Autowired
    protected IConfigurationService configurationService;

    protected FolderType folderType;

    protected Folder lastSelected;

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

        addButton = new Button("Add");
        addButton.setIcon(FontAwesome.FOLDER);
        addButton.setStyleName(ValoTheme.BUTTON_LINK);
        addButton.addClickListener(this);
        buttonLayout.addComponent(addButton);
        buttonLayout.setComponentAlignment(addButton, Alignment.MIDDLE_LEFT);

        delButton = new Button("Delete");
        delButton.setIcon(FontAwesome.FOLDER);
        delButton.setStyleName(ValoTheme.BUTTON_LINK);
        delButton.setEnabled(false);
        delButton.addClickListener(this);
        buttonLayout.addComponent(delButton);
        buttonLayout.setComponentAlignment(delButton, Alignment.MIDDLE_LEFT);

        addButtonsRight(buttonLayout);

        this.tree = new Tree();
        this.tree.setImmediate(true);
        this.tree.setMultiSelect(true);
        this.tree.setSelectable(true);
        this.tree.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public void valueChange(ValueChangeEvent event) {
                Set<Folder> selected = (Set<Folder>) tree.getValue();
                if (selected.size() > 0) {
                    lastSelected = selected.iterator().next();
                } else {
                    lastSelected = null;
                }
                delButton.setEnabled(selected.size() > 0);
            }
        });
        this.tree.addItemClickListener(new ItemClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                @SuppressWarnings("unchecked")
                Set<Folder> selected = (Set<Folder>) tree.getValue();
                if (selected.size() > 0) {
                    if (lastSelected != null && selected.iterator().next().equals(lastSelected)) {
                        tree.setValue(new HashSet<Folder>());
                    }
                }
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
                    tree.setItemIcon(event.getItemId(), FontAwesome.FOLDER_OPEN);
                }
            }
        });

        addComponent(tree);
        setExpandRatio(tree, 1);
    }

    protected void addButtonsRight(HorizontalLayout buttonLayout) {
    }

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

    @Override
    public void buttonClick(ClickEvent event) {
        Button button = event.getButton();
        if (button == addButton) {
            PromptDialog prompt = new PromptDialog("Add Folder", "Please choose a folder name",
                    this);
            UI.getCurrent().addWindow(prompt);
        } else if (button == delButton) {
            ConfirmDialog.show("Delete Folder?", "Are you sure you want to delete the folder?",
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
    }

    protected void deleteSelectedFolders() {
        @SuppressWarnings("unchecked")
        Set<Folder> folderIds = (Set<Folder>) tree.getValue();
        for (Folder folderId : folderIds) {
            configurationService.deleteFolder(folderId.getData().getId());
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
        if (children.size() == 0) {
            this.tree.setChildrenAllowed(folder, false);
        }
    }

}
