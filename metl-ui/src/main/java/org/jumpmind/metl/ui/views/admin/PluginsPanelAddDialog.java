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
package org.jumpmind.metl.ui.views.admin;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.plugin.IPluginManager;
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

    private ComboBox groupCombo;

    private ComboBox nameCombo;

    private TextField groupField;

    private TextField nameField;

    private TextField versionField;

    private List<PluginRepository> pluginRepositories;

    private UploadHandler uploadHandler;

    private byte[] jarContents;

    public PluginsPanelAddDialog(ApplicationContext context, PluginsPanel pluginsPanel) {
        super("Add Plugins");
        this.context = context;
        this.pluginsPanel = pluginsPanel;
        this.pluginRepositories = context.getPluginService().findPluginRepositories();

        TabSheet tabSheet = new TabSheet();

        AbstractLayout searchLayout = buildSearchLayout();
        tabSheet.addTab(searchLayout, "Search For New Versions");

        AbstractLayout uploadLayout = buildUploadLayout();
        tabSheet.addTab(uploadLayout, "Upload");

        searchButton = new Button("Search", e -> search());
        searchButton.setEnabled(false);

        uploadHandler = new UploadHandler();
        uploadButton = new Upload(null, uploadHandler);
        uploadButton.setImmediate(true);
        uploadButton.setVisible(false);
        uploadButton.setButtonCaption("Upload");
        uploadButton.addFinishedListener(e -> finishedUpload());

        tabSheet.addSelectedTabChangeListener(e -> {
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
        addButton.addClickListener(e -> addPlugin());

        addComponent(buildButtonFooter(uploadButton, searchButton, cancelButton, addButton));

        cancelButton.focus();

        setWidth(550, Unit.PIXELS);
        setHeight(300, Unit.PIXELS);
    }

    protected void addPlugin() {
        IPluginManager pluginManager = context.getPluginManager();
        if (searchButton.isVisible()) {
            Plugin newVersion = new Plugin((String)groupCombo.getValue(), (String)nameCombo.getValue(), (String)versionSelect.getValue(), 0);
            context.getPluginService().save(newVersion);
            pluginManager.refresh();
            close();
        } else if (uploadButton.isVisible() && jarContents != null) {
            try {
                File tempFile = File.createTempFile("install", ".jar");
                FileUtils.writeByteArrayToFile(tempFile, jarContents);
                pluginManager.install(groupField.getValue(), nameField.getValue(), versionField.getValue(), tempFile);
                tempFile.delete();
                close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }        
    }

    protected void finishedUpload() {
        jarContents = uploadHandler.os.toByteArray();
        uploadHandler.reset();
        if (isNotBlank(groupField.getValue()) && isNotBlank(nameField.getValue()) && isNotBlank(versionField.getValue())) {
            addButton.setEnabled(true);
        }
    }

    protected AbstractLayout buildSearchLayout() {
        FormLayout layout = new FormLayout();
        layout.setMargin(true);

        List<Plugin> existingPlugins = context.getPluginService().findPlugins();
        Set<String> groups = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Plugin plugin : existingPlugins) {
            groups.add(plugin.getArtifactGroup());
            names.add(plugin.getArtifactName());
        }

        versionSelect = new ListSelect("Versions");
        groupCombo = new ComboBox("Group");
        nameCombo = new ComboBox("Name");

        versionSelect.setRows(4);
        versionSelect.setMultiSelect(false);
        versionSelect.setNullSelectionAllowed(false);
        versionSelect.setWidth(100, Unit.PERCENTAGE);
        versionSelect.addValueChangeListener(e -> versionSelected());

        groupCombo.setWidth(100, Unit.PERCENTAGE);
        groupCombo.setNewItemsAllowed(true);
        groupCombo.addItems(groups);
        groupCombo.addValueChangeListener(e -> {
            populateNameField(nameCombo);
            setSearchButtonEnabled();
        });
        groupCombo.setNewItemHandler((newItemCaption) -> {
            groupCombo.removeItem(handEnteredGroup);
            handEnteredGroup = newItemCaption;
            groupCombo.addItem(handEnteredGroup);
            groupCombo.setValue(handEnteredGroup);
            setSearchButtonEnabled();
        });
        layout.addComponent(groupCombo);

        nameCombo.setWidth(100, Unit.PERCENTAGE);
        nameCombo.setNewItemsAllowed(true);
        nameCombo.addItems(names);
        nameCombo.addValueChangeListener(e -> {
            setSearchButtonEnabled();
        });
        nameCombo.setNewItemHandler((newItemCaption) -> {
            nameCombo.removeItem(handEnteredName);
            handEnteredName = newItemCaption;
            nameCombo.addItem(handEnteredName);
            nameCombo.setValue(handEnteredName);
            setSearchButtonEnabled();
        });
        layout.addComponent(nameCombo);

        layout.addComponent(versionSelect);

        return layout;
    }

    protected void versionSelected() {
        addButton.setEnabled(versionSelect.getValue() != null);
    }

    protected void search() {
        List<String> versions = context.getPluginManager().getAvailableVersions((String) groupCombo.getValue(), (String) nameCombo.getValue(),
                pluginRepositories);
        List<Plugin> plugins = context.getPluginService().findPlugins();
        for (Plugin plugin : plugins) {
            if (plugin.matches((String) groupCombo.getValue(), (String) nameCombo.getValue())) {
                versions.remove(plugin.getArtifactVersion());
            }
        }
        versionSelect.removeAllItems();
        versionSelect.addItems(versions);
    }

    protected void setSearchButtonEnabled() {
        searchButton.setEnabled(nameCombo.getValue() != null && groupCombo.getValue() != null);
        versionSelect.removeAllItems();
    }

    protected void populateNameField(ComboBox nameField) {

    }

    protected AbstractLayout buildUploadLayout() {
        FormLayout layout = new FormLayout();
        layout.setMargin(true);

        groupField = new TextField("Group");
        groupField.setWidth(100, Unit.PERCENTAGE);
        groupField.setRequired(true);
        layout.addComponent(groupField);

        nameField = new TextField("Name");
        nameField.setWidth(100, Unit.PERCENTAGE);
        nameField.setRequired(true);
        layout.addComponent(nameField);

        versionField = new TextField("Version");
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

    }

}
