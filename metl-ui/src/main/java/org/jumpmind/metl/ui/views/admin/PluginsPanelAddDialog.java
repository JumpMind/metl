package org.jumpmind.metl.ui.views.admin;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.themes.ValoTheme;

public class PluginsPanelAddDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    Button searchButton;

    PluginsPanel pluginsPanel;

    Upload uploadButton;

    public PluginsPanelAddDialog(ApplicationContext context, PluginsPanel pluginsPanel) {
        super("Add Plugins");
        this.context = context;
        this.pluginsPanel = pluginsPanel;

        TabSheet tabSheet = new TabSheet();

        AbstractLayout searchLayout = buildSearchLayout();
        tabSheet.addTab(searchLayout, "Search");

        AbstractLayout uploadLayout = buildUploadLayout();
        tabSheet.addTab(uploadLayout, "Upload");

        searchButton = new Button("Search");

        uploadButton = new Upload(null, new UploadHandler());
        uploadButton.setImmediate(true);
        uploadButton.setButtonCaption("Upload");

        tabSheet.addSelectedTabChangeListener((event) -> {
            boolean searchSelected = tabSheet.getSelectedTab().equals(searchLayout);
            searchButton.setVisible(searchSelected);
            uploadButton.setVisible(!searchSelected);
        });

        addComponent(tabSheet, 1);
        tabSheet.setSelectedTab(0);

        addComponent(buildButtonFooter(uploadButton, searchButton, buildCloseButton()));

        setWidth(550, Unit.PIXELS);
        setHeight(300, Unit.PIXELS);
    }
    
    protected AbstractLayout buildSearchLayout() {
        FormLayout layout = new FormLayout();
        layout.setMargin(true);
        
        ComboBox groupField = new ComboBox("Group");
        groupField.setWidth(100, Unit.PERCENTAGE);
        groupField.setNewItemsAllowed(true);
        layout.addComponent(groupField);
        
        ComboBox nameField = new ComboBox("Name");
        nameField.setWidth(100, Unit.PERCENTAGE);
        nameField.setNewItemsAllowed(true);
        layout.addComponent(nameField);
        
        return layout;
    }
    
    protected AbstractLayout buildUploadLayout() {
        FormLayout layout = new FormLayout();
        layout.setMargin(true);
        
        TextField groupField = new TextField("Group");
        groupField.setWidth(100, Unit.PERCENTAGE);
        groupField.setRequired(true);
        layout.addComponent(groupField);
        
        TextField nameField = new TextField("Name");
        nameField.setWidth(100, Unit.PERCENTAGE);
        nameField.setRequired(true);
        layout.addComponent(nameField);
        
        TextField versionField = new TextField("Version");
        versionField.setWidth(100, Unit.PERCENTAGE);
        versionField.setRequired(true);
        layout.addComponent(versionField);
        
        return layout;
    }

    protected HorizontalLayout buildButtonFooter(AbstractComponent... toTheRightButtons) {
        HorizontalLayout footer = new HorizontalLayout();

        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);

        Label footerText = new Label("");
        footerText.setSizeUndefined();

        footer.addComponents(footerText);
        footer.setExpandRatio(footerText, 1);

        if (toTheRightButtons != null) {
            footer.addComponents(toTheRightButtons);
        }

        return footer;
    }

    @Override
    protected boolean onClose() {
        pluginsPanel.refresh();
        return super.onClose();
    }

    class UploadHandler implements Receiver {

        private static final long serialVersionUID = 1L;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        public OutputStream receiveUpload(String filename, String mimeType) {
            return os;
        }

        public void reset() {
            os = new ByteArrayOutputStream();
        }

        public String getContent() {
            return os.toString();
        }

    }

}
