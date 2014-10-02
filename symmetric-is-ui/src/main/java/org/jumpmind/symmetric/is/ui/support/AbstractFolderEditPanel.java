package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.FolderData;
import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.core.persist.ConfigurationService;
import org.jumpmind.symmetric.is.ui.support.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.is.ui.support.PromptDialog.IPromptListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class AbstractFolderEditPanel extends VerticalLayout implements ClickListener,
        IPromptListener {

    private static final long serialVersionUID = 1L;

    protected TreeTable treeTable;

    protected Button addButton;

    protected Button delButton;

    @Autowired
    protected ConfigurationService configurationService;

    protected FolderType folderType;

    protected String lastSelectedId;

    public AbstractFolderEditPanel(String title, FolderType folderType) {
        this.folderType = folderType;

        setMargin(true);
        setSpacing(true);
        setSizeFull();

        Label titleLabel = new Label(title);
        titleLabel.addStyleName("h2");
        addComponent(titleLabel);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        addComponent(buttonLayout);

        addButton = new Button(FontAwesome.PLUS);
        addButton.setStyleName(ValoTheme.BUTTON_BORDERLESS);
        addButton.addClickListener(this);
        buttonLayout.addComponent(addButton);
        buttonLayout.setComponentAlignment(addButton, Alignment.MIDDLE_LEFT);

        delButton = new Button(FontAwesome.MINUS);
        delButton.setStyleName(ValoTheme.BUTTON_BORDERLESS);
        delButton.setEnabled(false);
        delButton.addClickListener(this);
        buttonLayout.addComponent(delButton);
        buttonLayout.setComponentAlignment(delButton, Alignment.MIDDLE_LEFT);

        this.treeTable = new TreeTable();
        this.treeTable.setImmediate(true);
        this.treeTable.setMultiSelect(true);
        this.treeTable.setSelectable(true);
        this.treeTable.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked")
            @Override
            public void valueChange(ValueChangeEvent event) {
                Set<String> selected = (Set<String>) treeTable.getValue();
                if (selected.size() > 0) {
                    lastSelectedId = selected.iterator().next();
                } else {
                    lastSelectedId = null;
                }
                delButton.setEnabled(selected.size() > 0);
            }
        });
        this.treeTable.addItemClickListener(new ItemClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                @SuppressWarnings("unchecked")
                Set<String> selected = (Set<String>) treeTable.getValue();
                if (selected.size() > 0) {
                    if (lastSelectedId != null && selected.iterator().next().equals(lastSelectedId)) {
                        treeTable.setValue(new HashSet<String>());
                    }
                }
            }
        });
        this.treeTable.setSizeFull();
        this.treeTable.addContainerProperty("name", String.class, null, "Name", null, null);

        addComponent(treeTable);
        setExpandRatio(treeTable, 1);
    }

    @Override
    public boolean onOk(String content) {
        if (isNotBlank(content)) {
            String parentId = null;
            @SuppressWarnings("unchecked")
            Set<String> selectedIds = (Set<String>) treeTable.getValue();
            if (selectedIds != null && selectedIds.size() > 0) {
                parentId = selectedIds.iterator().next();
            }
            FolderData folderData = new FolderData();
            folderData.setName(content);
            folderData.setType(folderType.name());
            folderData.setParentFolderId(parentId);
            configurationService.save(new Folder(folderData));
            refresh();
            treeTable.setCollapsed(parentId, false);
            Set<String> selected = new HashSet<String>();
            selected.add(folderData.getId());
            treeTable.setValue(selected);
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
        Set<String> folderIds = (Set<String>) treeTable.getValue();
        for (String folderId : folderIds) {
            configurationService.deleteFolder(folderId);
        }
    }

    public void refresh() {
        this.treeTable.removeAllItems();
        List<Folder> folders = configurationService.findFolders(folderType);
        for (Folder folder : folders) {
            addChildFolder(folder);
        }
    }

    protected void addChildFolder(Folder folder) {
        String id = folder.getData().getId();
        this.treeTable.addItem(new Object[] { folder.getData().getName() }, id);
        if (folder.getParent() != null) {
            this.treeTable.setParent(id, folder.getParent().getData().getId());
        }
        List<Folder> children = folder.getChildren();
        for (Folder child : children) {
            addChildFolder(child);
        }
        if (children.size() == 0) {
            this.treeTable.setChildrenAllowed(id, false);
        }
    }

}
