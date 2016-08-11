package org.jumpmind.metl.ui.views.admin;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.persist.IConfigurationService;
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
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.themes.ValoTheme;

public class PluginsPanelAddDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    final Logger logger = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    Button searchButton;

    PluginsPanel pluginsPanel;

    Upload uploadButton;

    Button cancelButton;

    Button addButton;

    String handEnteredGroup;

    String handEnteredName;

    private ListSelect versionSelect;

    private ComboBox groupField;

    private ComboBox nameField;
    
    private List<PluginRepository> pluginRepositories;

    public PluginsPanelAddDialog(ApplicationContext context, PluginsPanel pluginsPanel) {
        super("Add Plugins");
        this.context = context;
        this.pluginsPanel = pluginsPanel;
        this.pluginRepositories = context.getConfigurationService().findPluginRepositories();

        TabSheet tabSheet = new TabSheet();

        AbstractLayout searchLayout = buildSearchLayout();
        tabSheet.addTab(searchLayout, "Search For New Versions");

        AbstractLayout uploadLayout = buildUploadLayout();
        tabSheet.addTab(uploadLayout, "Upload");

        searchButton = new Button("Search", (event)->search());
        searchButton.setEnabled(false);

        uploadButton = new Upload(null, new UploadHandler());
        uploadButton.setImmediate(true);
        uploadButton.setVisible(false);
        uploadButton.setButtonCaption("Upload");

        tabSheet.addSelectedTabChangeListener((event) -> {
            boolean searchSelected = tabSheet.getSelectedTab().equals(searchLayout);
            searchButton.setVisible(searchSelected);
            uploadButton.setVisible(!searchSelected);
        });

        addComponent(tabSheet, 1);
        tabSheet.setSelectedTab(0);

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new CloseButtonListener());

        addButton = new Button("Add");
        addButton.setEnabled(false);
        addButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        addComponent(buildButtonFooter(uploadButton, searchButton, cancelButton, addButton));

        cancelButton.focus();

        setWidth(550, Unit.PIXELS);
        setHeight(300, Unit.PIXELS);
    }

    protected AbstractLayout buildSearchLayout() {
        FormLayout layout = new FormLayout();
        layout.setMargin(true);

        IConfigurationService configurationService = context.getConfigurationService();
        List<Plugin> existingPlugins = configurationService.findPlugins();
        Set<String> groups = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Plugin plugin : existingPlugins) {
            groups.add(plugin.getArtifactGroup());
            names.add(plugin.getArtifactName());
        }

        versionSelect = new ListSelect("Versions");
        groupField = new ComboBox("Group");
        nameField = new ComboBox("Name");

        versionSelect.setRows(4);
        versionSelect.setNullSelectionAllowed(false);
        versionSelect.setWidth(100, Unit.PERCENTAGE);
        versionSelect.addValueChangeListener((e)-> versionSelected());

        groupField.setWidth(100, Unit.PERCENTAGE);
        groupField.setNewItemsAllowed(true);
        groupField.addItems(groups);
        groupField.addValueChangeListener((event) -> {
            populateNameField(nameField);
            setSearchButtonEnabled();
        });
        groupField.setNewItemHandler((newItemCaption) -> {
            groupField.removeItem(handEnteredGroup);
            handEnteredGroup = newItemCaption;
            groupField.addItem(handEnteredGroup);
            groupField.setValue(handEnteredGroup);
            setSearchButtonEnabled();
        });
        layout.addComponent(groupField);

        nameField.setWidth(100, Unit.PERCENTAGE);
        nameField.setNewItemsAllowed(true);
        nameField.addItems(names);
        nameField.addValueChangeListener((event) -> {
            populateGroupField(groupField);
            setSearchButtonEnabled();
        });
        nameField.setNewItemHandler((newItemCaption) -> {
            nameField.removeItem(handEnteredName);
            handEnteredName = newItemCaption;
            nameField.addItem(handEnteredName);
            nameField.setValue(handEnteredName);
            setSearchButtonEnabled();
        });
        layout.addComponent(nameField);

        layout.addComponent(versionSelect);

        return layout;
    }
    
    protected void versionSelected() {
        
    }
    
    protected void search() {
        List<String> versions = context.getPluginManager().getAvailableVersions((String)groupField.getValue(), (String)nameField.getValue(), pluginRepositories);
        List<Plugin> plugins = context.getConfigurationService().findPlugins();
        for (Plugin plugin : plugins) {
            if (plugin.matches((String)groupField.getValue(), (String)nameField.getValue())) {
                versions.remove(plugin.getArtifactVersion());
            }
        }
    }
    
    protected void setSearchButtonEnabled() {
        searchButton.setEnabled(nameField.getValue() != null && groupField.getValue() != null);    
        versionSelect.removeAllItems();
    }

    protected void populateNameField(ComboBox nameField) {

    }

    protected void populateGroupField(ComboBox groupField) {

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
