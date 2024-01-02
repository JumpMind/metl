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
package org.jumpmind.metl.ui.views.design;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.XPathXmlParser;
import org.jumpmind.metl.core.runtime.component.XmlFormatter;
import org.jumpmind.metl.core.runtime.component.XmlParser;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;
import org.jumpmind.metl.ui.views.design.ImportXmlTemplateWindow.ImportXmlListener;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.components.grid.HeaderCell;
import com.vaadin.ui.components.grid.HeaderRow;
import com.vaadin.ui.themes.ValoTheme;

public class EditXmlFormatPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;
    
    List<Record> recordList = new ArrayList<Record>();

    Grid<Record> grid;
    
    TextField entityFilterField = new TextField();
    
    TextField attributeFilterField = new TextField();
    
    CheckBox filterCheckBox = new CheckBox("Show Set Only");

    Set<String> xpathChoices;
    
    ComboBox<String> xpathCombo = new ComboBox<String>();

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button editButton = buttonBar.addButton("Edit Template", VaadinIcons.FILE_CODE);
        editButton.addClickListener(new EditTemplateClickListener());

        Button importButton = buttonBar.addButton("Import Template", VaadinIcons.DOWNLOAD);
        importButton.addClickListener(new ImportTemplateClickListener());

        buttonBar.addButtonRight("Export", VaadinIcons.DOWNLOAD, (e) -> export());

        buildGrid();
        refresh();
        saveXPathSettings();
        buildXpathChoices();

    }

    protected void export() {
        ExportDialog.show(context, grid);
    }

    @SuppressWarnings("unchecked")
	protected void buildGrid() {
        grid = new Grid<Record>();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.setSizeFull();
        grid.addColumn(Record::getEntityName).setId("entityName").setCaption("Entity Name");
        grid.addColumn(Record::getAttributeName).setId("attributeName").setCaption("Attribute Name");
        grid.addColumn(Record::getXpath).setId("xpath").setCaption("Xpath").setExpandRatio(1);
        HeaderRow filterRow = grid.appendHeaderRow();

        addColumn("entityName", filterRow, entityFilterField);

        addColumn("attributeName", filterRow, attributeFilterField);

        if (!readOnly) {
            xpathCombo.addValueChangeListener(e->saveXPathSettings());
            xpathCombo.setWidth(100, Unit.PERCENTAGE);
            xpathCombo.setNewItemProvider(newItem -> {
    			List<String> itemList = xpathCombo.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
    			itemList.add(newItem);
    			xpathCombo.setItems(itemList);
    			xpathCombo.setValue(newItem);
    			return Optional.of(newItem);
            });
            xpathCombo.setTextInputAllowed(true);
            xpathCombo.setScrollToSelectedItem(true);
            ((Column<Record, String>) grid.getColumn("xpath")).setEditorComponent(xpathCombo, Record::setXpath);
            grid.getEditor().setEnabled(true).setBuffered(false);
        }
        addShowPopulatedFilter("xpath", filterRow);
        addComponent(grid);
        setExpandRatio(grid, 1);
    }

    protected void refresh() {
        RelationalModel model = null;
        // View is used by multiple components.
        // What model does the component use?
        if (component.getType().equals(XmlParser.TYPE)) {
            model = (RelationalModel) component.getOutputModel();
        } else if (component.getType().equals(XPathXmlParser.TYPE)) {
            model = (RelationalModel) component.getOutputModel();
        } else if (component.getType().equals(XmlFormatter.TYPE)){
            model = (RelationalModel) component.getInputModel();
        } else {
            model = (RelationalModel) component.getInputModel();
        }
        
        if (model != null) {
            Collections.sort(model.getModelEntities(), new Comparator<ModelEntity>() {
                public int compare(ModelEntity entity1, ModelEntity entity2) {
                    return entity1.getName().toLowerCase()
                            .compareTo(entity2.getName().toLowerCase());
                }
            });

            for (ModelEntity entity : model.getModelEntities()) {
                boolean firstAttribute = true;
                Record entityRecord = new Record(entity, null);
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    if (firstAttribute) {
                        firstAttribute = false;
                        recordList.add(entityRecord);
                    }
                    recordList.add(new Record(entity, attr));
                }
                if (firstAttribute) {
                    recordList.add(entityRecord);
                }
            }
        }
        refreshGrid();
    }
    
    protected void refreshGrid() {
    	List<Record> filteredRecordList = new ArrayList<Record>();
    	for (Record record : recordList) {
    		if (!isFilteredOut(record)) {
    			filteredRecordList.add(record);
    		}
    	}
    	grid.setItems(filteredRecordList);
    }
    
    protected boolean isFilteredOut(Record record) {
		boolean validEntity = StringUtils.isBlank(entityFilterField.getValue()) || (record.getEntityName() != null
				&& record.getEntityName().toLowerCase().contains(entityFilterField.getValue().toLowerCase()));
		boolean validAttribute = StringUtils.isBlank(attributeFilterField.getValue()) || (record.getAttributeName() != null
				&& record.getAttributeName().toLowerCase().contains(attributeFilterField.getValue().toLowerCase()));
    	boolean validXpath = StringUtils.isNotBlank(record.getXpath());
    	return !validEntity || !validAttribute || !validXpath;
    }
    
    protected void addShowPopulatedFilter(String propertyId, HeaderRow filterRow) {
        HeaderCell cell = filterRow.getCell(propertyId);
        filterCheckBox.addValueChangeListener(l->refreshGrid());
        filterCheckBox.addStyleName(ValoTheme.CHECKBOX_SMALL);
        cell.setComponent(filterCheckBox);
        
    }

    protected void addColumn(String propertyId, HeaderRow filterRow, TextField filterField) {
        HeaderCell cell = filterRow.getCell(propertyId);
        filterField.setPlaceholder("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        filterField.setWidth(100, Unit.PERCENTAGE);
        filterField.addValueChangeListener(change -> refreshGrid());
        cell.setComponent(filterField);
    }

    protected void saveXPathSettings() {
        for (Record record : recordList) {
            if (record.modelAttribute != null) {
                saveAttributeSetting(record.modelAttribute.getId(),
                        XmlFormatter.XML_FORMATTER_XPATH,
                        StringUtils.trimToNull(record.getXpath()));
            } else {
                saveEntitySetting(record.modelEntity.getId(), XmlFormatter.XML_FORMATTER_XPATH,
                        StringUtils.trimToNull(record.getXpath()));
            }
        }
    }

    protected void saveAttributeSetting(String attributeId, String name, String value) {
        ComponentAttribSetting setting = component.getSingleAttributeSetting(attributeId, name);
        if (setting == null && value != null) {
            setting = new ComponentAttribSetting(attributeId, name, value);
            setting.setComponentId(component.getId());
            component.addAttributeSetting(setting);
            context.getConfigurationService().save(setting);
        } else if (setting != null && !StringUtils.equals(setting.getValue(), value)) {
            if (value == null) {
                setting.setValue(value);
                context.getConfigurationService().delete(setting);
            } else {
                setting.setValue(value);
                context.getConfigurationService().save(setting);
            }
        }
    }

    protected void saveEntitySetting(String entityId, String name, String value) {
        ComponentEntitySetting setting = component.getSingleEntitySetting(entityId, name);
        if (setting == null && value != null) {
            setting = new ComponentEntitySetting(entityId, name, value);
            setting.setComponentId(component.getId());
            component.addEntitySetting(setting);
            context.getConfigurationService().save(setting);
        } else if (setting != null && !StringUtils.equals(setting.getValue(), value)) {
            if (value == null) {
                setting.setValue(value);
                context.getConfigurationService().delete(setting);
            } else {
                setting.setValue(value);
                context.getConfigurationService().save(setting);
            }
        }
    }

    protected void buildXpathChoices() {
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        Setting setting = component.findSetting(XmlFormatter.XML_FORMATTER_TEMPLATE);
        xpathChoices = new TreeSet<String>();
        if (StringUtils.isNotBlank(setting.getValue())) {
            try {
                Document document = builder.build(new StringReader(setting.getValue()));
                
                buildXpathChoicesFromElement("/" + document.getRootElement().getName(),
                        document.getRootElement());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        xpathCombo.setItems(xpathChoices);
    }

    protected void buildXpathChoicesFromElement(String prefix, Element parentElement) {
        for (Element element : parentElement.getChildren()) {
            String text = prefix + "/" + element.getName();
            xpathChoices.add(text);
            for (Attribute attr : element.getAttributes()) {
                String attrText = text + "/@" + attr.getName();
                xpathChoices.add(attrText);
            }
            buildXpathChoicesFromElement(text, element);
        }
    }

    class EditTemplateClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;

        public void buttonClick(ClickEvent event) {
            EditTemplateWindow window = new EditTemplateWindow();
            window.show();
        }
    }

    class EditTemplateWindow extends ResizableWindow {
        private static final long serialVersionUID = 1L;

        AceEditor editor;

        public EditTemplateWindow() {
            super("Edit XML Template");
            setWidth(800f, Unit.PIXELS);
            setHeight(600f, Unit.PIXELS);
            content.setMargin(true);

            editor = new AceEditor();
            editor.setMode(AceMode.xml);
            editor.setSizeFull();
            editor.setHighlightActiveLine(true);
            editor.setShowPrintMargin(false);
            addComponent(editor);
            content.setExpandRatio(editor, 1.0f);

            Setting templateSetting = component.findSetting(XmlFormatter.XML_FORMATTER_TEMPLATE);
            editor.setValue(templateSetting.getValue());
            editor.setReadOnly(readOnly);

            addComponent(buildButtonFooter(buildCloseButton()));
        }

        @Override
        public void close() {
            super.close();
            Setting templateSetting = component.findSetting(XmlFormatter.XML_FORMATTER_TEMPLATE);
            templateSetting.setValue(editor.getValue());
            context.getConfigurationService().save(templateSetting);
            buildXpathChoices();
            refresh();
        }

    }

    class ImportTemplateClickListener implements ClickListener, ImportXmlListener {
        private static final long serialVersionUID = 1L;

        ImportXmlTemplateWindow importWindow;

        public void buttonClick(ClickEvent event) {
            importWindow = new ImportXmlTemplateWindow(this, component, context);
            UI.getCurrent().addWindow(importWindow);
        }

        public void onImport(String xml) {
            Setting templateSetting = component.findSetting(XmlFormatter.XML_FORMATTER_TEMPLATE);
            templateSetting.setValue(xml);
            context.getConfigurationService().save(templateSetting);
            importWindow.close();
            EditTemplateWindow editWindow = new EditTemplateWindow();
            editWindow.show();
        }
    }

    public class Record implements Serializable {

        private static final long serialVersionUID = 1L;

        ModelEntity modelEntity;

        ModelAttrib modelAttribute;

        String xpath = "";

        public Record(ModelEntity modelEntity, ModelAttrib modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;
            if (modelAttribute != null) {
                ComponentAttribSetting setting = component.getSingleAttributeSetting(
                        modelAttribute.getId(), XmlFormatter.XML_FORMATTER_XPATH);
                if (setting != null) {
                    xpath = setting.getValue();
                }
            } else {
                ComponentEntitySetting setting = component.getSingleEntitySetting(
                        modelEntity.getId(), XmlFormatter.XML_FORMATTER_XPATH);
                if (setting != null) {
                    xpath = setting.getValue();
                }
            }
        }

        public int hashCode() {
            return modelEntity.hashCode()
                    + (modelAttribute == null ? 0 : modelAttribute.hashCode());
        }

        public boolean equals(Object obj) {
            if (obj instanceof Record) {
                return hashCode() == ((Record) obj).hashCode();
            }
            return super.equals(obj);
        }

        public String getEntityName() {
            return modelEntity.getName();
        }

        public String getAttributeName() {
            if (modelAttribute != null) {
                return modelAttribute.getName();
            }
            return null;
        }

        public String getXpath() {
            return xpath;
        }

        public void setXpath(String xpath) {
            this.xpath = xpath;
        }
    }
}
