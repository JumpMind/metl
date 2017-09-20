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
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.XPathXmlParser;
import org.jumpmind.metl.core.runtime.component.XmlFormatter;
import org.jumpmind.metl.core.runtime.component.XmlParser;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.views.design.ImportXmlTemplateWindow.ImportXmlListener;
import org.jumpmind.vaadin.ui.common.ExportDialog;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.filter.And;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.IsNull;
import com.vaadin.data.util.filter.Not;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

public class EditXmlFormatPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;

    Grid grid;

    Set<String> xpathChoices;

    BeanItemContainer<Record> container;

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button editButton = buttonBar.addButton("Edit Template", FontAwesome.FILE_CODE_O);
        editButton.addClickListener(new EditTemplateClickListener());

        Button importButton = buttonBar.addButton("Import Template", FontAwesome.DOWNLOAD);
        importButton.addClickListener(new ImportTemplateClickListener());

        buttonBar.addButtonRight("Export", FontAwesome.DOWNLOAD, (e) -> export());

        buildGrid();
        refresh();
        saveXPathSettings();
        buildXpathChoices();

    }

    protected void export() {
        String fileNamePrefix = component.getName().toLowerCase().replace(' ', '-');
        ExportDialog dialog = new ExportDialog(grid, fileNamePrefix, component.getName());
        UI.getCurrent().addWindow(dialog);
    }

    protected void buildGrid() {
        grid = new Grid();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.setSizeFull();
        grid.setEditorEnabled(!readOnly);
        container = new BeanItemContainer<Record>(Record.class);
        grid.setContainerDataSource(container);
        grid.setColumns("entityName", "attributeName", "xpath");
        HeaderRow filterRow = grid.appendHeaderRow();

        addColumn("entityName", filterRow);

        addColumn("attributeName", filterRow);

        ComboBox combo = new ComboBox();
        combo.addValueChangeListener(e->saveXPathSettings());
        combo.setWidth(100, Unit.PERCENTAGE);
        combo.setImmediate(true);
        combo.setNewItemsAllowed(true);
        combo.setInvalidAllowed(true);
        combo.setTextInputAllowed(true);
        combo.setScrollToSelectedItem(true);
        combo.setFilteringMode(FilteringMode.CONTAINS);
        grid.getColumn("xpath").setEditorField(combo).setExpandRatio(1);
        addShowPopulatedFilter("xpath", filterRow);
        grid.setEditorBuffered(false);
        addComponent(grid);
        setExpandRatio(grid, 1);
    }

    protected void refresh() {
        Model model = null;
        // View is used by multiple components.
        // What model does the component use?
        if (component.getType().equals(XmlParser.TYPE)) {
            model = component.getOutputModel();
        } else if (component.getType().equals(XPathXmlParser.TYPE)) {
            model = component.getOutputModel();
        } else if (component.getType().equals(XmlFormatter.TYPE)){
            model = component.getInputModel();
        } else {
            model = component.getInputModel();
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
                        container.addItem(entityRecord);
                    }
                    container.addItem(new Record(entity, attr));
                }
                if (firstAttribute) {
                    container.addItem(entityRecord);
                }
            }
        }
    }
    
    protected void addShowPopulatedFilter(String propertyId, HeaderRow filterRow) {
        HeaderCell cell = filterRow.getCell(propertyId);
        CheckBox group = new CheckBox("Show Set Only");
        group.setImmediate(true);
        group.addValueChangeListener(l->{
            container.removeContainerFilters(propertyId);
            if (group.getValue()) {
                container.addContainerFilter(new And(new Not(new Compare.Equal(propertyId,"")), new Not(new IsNull(propertyId))));
            }
        });
        group.addStyleName(ValoTheme.CHECKBOX_SMALL);
        cell.setComponent(group);
        
    }

    protected void addColumn(String propertyId, HeaderRow filterRow) {
        grid.getColumn(propertyId).setEditable(false);
        HeaderCell cell = filterRow.getCell(propertyId);
        TextField filterField = new TextField();
        filterField.setInputPrompt("Filter");
        filterField.setImmediate(true);
        filterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        filterField.setWidth(100, Unit.PERCENTAGE);
        filterField.addTextChangeListener(change -> {
            container.removeContainerFilters(propertyId);
            if (!change.getText().isEmpty()) {
                container.addContainerFilter(
                        new SimpleStringFilter(propertyId, change.getText(), true, false));
            }
        });
        cell.setComponent(filterField);
    }

    protected void saveXPathSettings() {
        for (Object obj : container.getItemIds()) {
            Record record = (Record) obj;
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

        ComboBox combo = (ComboBox) grid.getColumn("xpath").getEditorField();
        combo.removeAllItems();
        combo.addItems(xpathChoices);
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
            editor.setImmediate(true);
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
