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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ResizableDialog;
import org.jumpmind.vaadin.ui.common.TabSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.Upload;

public class PluginsPanelAddDialog extends ResizableDialog {

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

    private ListBox<String> versionSelect;
    
    private boolean allowEmptyVersionSelection = true;

    private ComboBox<String> groupCombo;

    private ComboBox<String> nameCombo;

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

        FormLayout searchLayout = buildSearchLayout();
        tabSheet.add(searchLayout, "Search For New Versions");

        FormLayout uploadLayout = buildUploadLayout();
        tabSheet.add(uploadLayout, "Upload");

        searchButton = new Button("Search", e -> search());
        searchButton.setEnabled(false);

        uploadHandler = new UploadHandler();
        uploadButton = new Upload(uploadHandler);
        uploadButton.setVisible(false);
        uploadButton.addFinishedListener(e -> finishedUpload());

        tabSheet.addSelectedTabChangeListener(e -> {
            boolean searchSelected = tabSheet.getSelectedTab().equals(searchLayout);
            searchButton.setVisible(searchSelected);
            uploadButton.setVisible(!searchSelected);
        });

        add(tabSheet, 1);
        tabSheet.setSelectedTab(0);

        cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new CloseButtonListener());

        addButton = new Button("Add");
        addButton.setEnabled(false);
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> addPlugin());

        add(buildButtonFooter(uploadButton, searchButton, cancelButton, addButton));

        cancelButton.focus();

        setWidth("550px");
        setHeight("300px");
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

    protected FormLayout buildSearchLayout() {
        FormLayout layout = new FormLayout();

        List<Plugin> existingPlugins = context.getPluginService().findPlugins();
        Set<String> groups = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Plugin plugin : existingPlugins) {
            groups.add(plugin.getArtifactGroup());
            names.add(plugin.getArtifactName());
        }

        versionSelect = new ListBox<String>();
        groupCombo = new ComboBox<String>("Group");
        nameCombo = new ComboBox<String>("Name");

        versionSelect.setHeight("144px");
        //versionSelect.setVisibleItemCount(4);
        versionSelect.setWidthFull();
        versionSelect.addValueChangeListener(e -> versionSelected(e));

        groupCombo.setWidthFull();
        groupCombo.setItems(groups);
        groupCombo.addValueChangeListener(e -> {
            populateNameField(nameCombo);
            setSearchButtonEnabled();
        });
        groupCombo.setAllowCustomValue(true);
        groupCombo.addCustomValueSetListener(event -> {
        	groups.remove(handEnteredGroup);
        	handEnteredGroup = event.getDetail();
        	groups.add(handEnteredGroup);
        	groupCombo.setItems(groups);
        	groupCombo.setValue(handEnteredGroup);
        	setSearchButtonEnabled();
        });
        layout.add(groupCombo);

        nameCombo.setWidthFull();
        nameCombo.setItems(names);
        nameCombo.addValueChangeListener(e -> {
            setSearchButtonEnabled();
        });
        nameCombo.setAllowCustomValue(true);
        nameCombo.addCustomValueSetListener(event -> {
        	names.remove(handEnteredName);
        	handEnteredName = event.getDetail();
        	names.add(handEnteredName);
        	nameCombo.setItems(names);
        	nameCombo.setValue(handEnteredName);
        	setSearchButtonEnabled();
        });
        layout.add(nameCombo);
        layout.addFormItem(versionSelect, "Versions");

        return layout;
    }

    protected void versionSelected(ValueChangeEvent<String> event) {
        if (event.getValue() != null) {
            addButton.setEnabled(true);
        } else {
            addButton.setEnabled(false);
            if (!allowEmptyVersionSelection && event.getOldValue() != null) {
                versionSelect.setValue(event.getOldValue());
            }
        }
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
        versionSelect.setItems(versions);
        if (!versions.isEmpty()) {
            allowEmptyVersionSelection = false;
            versionSelect.setValue(versions.iterator().next());
        }
    }

    protected void setSearchButtonEnabled() {
        searchButton.setEnabled(nameCombo.getValue() != null && groupCombo.getValue() != null);
        allowEmptyVersionSelection = true;
        versionSelect.setItems(new ArrayList<String>());
    }

    protected void populateNameField(ComboBox<?> nameField) {

    }

    protected FormLayout buildUploadLayout() {
        FormLayout layout = new FormLayout();

        groupField = new TextField("Group");
        groupField.setWidthFull();
        groupField.setRequiredIndicatorVisible(true);
        layout.add(groupField);

        nameField = new TextField("Name");
        nameField.setWidthFull();
        nameField.setRequiredIndicatorVisible(true);
        layout.add(nameField);

        versionField = new TextField("Version");
        versionField.setWidthFull();
        versionField.setRequiredIndicatorVisible(true);
        layout.add(versionField);

        return layout;
    }

    protected HorizontalLayout buildButtonFooter(Component... toTheRightButtons) {
        HorizontalLayout footer = new HorizontalLayout();

        footer.setWidth("100%");
        footer.setSpacing(true);

        Span footerText = new Span("");
        footerText.setSizeUndefined();

        footer.addAndExpand(footerText);

        if (toTheRightButtons != null) {
            footer.add(toTheRightButtons);
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
