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

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.XmlFormatter;
import org.jumpmind.metl.core.runtime.component.XmlParser;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.views.design.ImportXmlTemplateWindow.ImportXmlListener;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class EditXmlFormatPanel extends AbstractComponentEditPanel implements TextChangeListener {

    Table table = new Table();

    BeanItemContainer<Record> container = new BeanItemContainer<Record>(Record.class);

    TextField filterField;

    Set<String> xpathChoices;

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button editButton = buttonBar.addButton("Edit Template", FontAwesome.FILE_CODE_O);
        editButton.addClickListener(new EditTemplateClickListener());

        Button importButton = buttonBar.addButton("Import Template", FontAwesome.DOWNLOAD);
        importButton.addClickListener(new ImportTemplateClickListener());

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(this);

        table.setContainerDataSource(container);
        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSizeFull();
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "xpath" });
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "XPath" });
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        addComponent(table);
        setExpandRatio(table, 1.0f);

        updateTable(null);
        saveXPathSettings();
        buildXpathChoices();
    }

    @Override
    public void textChange(TextChangeEvent event) {
        filterField.setValue(event.getText());
        updateTable(event.getText());
    }

    protected void updateTable(String filterText) {
        Model model = component.getType().equals(XmlParser.TYPE) ? component.getOutputModel() : component.getInputModel();
        if (model != null) {
            table.removeAllItems();
            String upperFilterText = StringUtils.trimToEmpty(filterText).toUpperCase();
            for (ModelEntity entity : model.getModelEntities()) {
                boolean firstAttribute = true;
                boolean entityMatches = upperFilterText.equals("") || entity.getName().toUpperCase().indexOf(upperFilterText) >= 0;
                for (ModelAttribute attr : entity.getModelAttributes()) {
                    if (entityMatches || attr.getName().toUpperCase().indexOf(upperFilterText) >= 0) {
                        if (firstAttribute) {
                            firstAttribute = false;
                            table.addItem(new Record(entity, null));
                        }
                        table.addItem(new Record(entity, attr));
                    }
                }
                if (entityMatches && firstAttribute) {
                    table.addItem(new Record(entity, null));
                }
            }
        }
    }

    protected void saveXPathSettings() {
        for (Record record : container.getItemIds()) {
            if (record.getAttributeId() != null) {
                saveAttributeSetting(record.getAttributeId(), XmlFormatter.XML_FORMATTER_XPATH, StringUtils.trimToNull(record.getXpath()));
            } else {
                saveEntitySetting(record.getEntityId(), XmlFormatter.XML_FORMATTER_XPATH, StringUtils.trimToNull(record.getXpath()));
            }
        }
    }

    protected void saveAttributeSetting(String attributeId, String name, String value) {
        ComponentAttributeSetting setting = component.getSingleAttributeSetting(attributeId, name);
        if (setting == null && value != null) {
            setting = new ComponentAttributeSetting(attributeId, name, value);
            setting.setComponentId(component.getId());
            component.addAttributeSetting(setting);
            context.getConfigurationService().save(setting);
        } else if (setting != null && !StringUtils.equals(setting.getValue(), value)) {
            if (value == null) {
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
        if (StringUtils.isNotBlank(setting.getValue())) {
            try {
                Document document = builder.build(new StringReader(setting.getValue()));
                xpathChoices = new HashSet<String>();
                buildXpathChoicesFromElement("/" + document.getRootElement().getName(), document.getRootElement());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
        public void buttonClick(ClickEvent event) {
            EditTemplateWindow window = new EditTemplateWindow();
            window.show();
        }
    }

    class EditTemplateWindow extends ResizableWindow {
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

            addComponent(buildButtonFooter(buildCloseButton()));
        }

        protected boolean onClose() {
            Setting templateSetting = component.findSetting(XmlFormatter.XML_FORMATTER_TEMPLATE);
            templateSetting.setValue(editor.getValue());
            context.getConfigurationService().save(templateSetting);
            buildXpathChoices();
            updateTable(filterField.getValue());
            return true;
        }
    }

    class ImportTemplateClickListener implements ClickListener, ImportXmlListener {
        ImportXmlTemplateWindow importWindow;

        public void buttonClick(ClickEvent event) {
            importWindow = new ImportXmlTemplateWindow(this);
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

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            Field<?> field = null;
            Record record = (Record) itemId;
            if (propertyId.equals("xpath")) {
                final ComboBox combo = new ComboBox();
                combo.setWidth(100, Unit.PERCENTAGE);
                if (xpathChoices != null) {
                    combo.addItems(xpathChoices);
                }
                if (!StringUtils.trimToEmpty(record.getXpath()).equals("") && !combo.getItemIds().contains(record.getXpath())) {
                    combo.addItem(record.getXpath());
                }
                combo.setPageLength(20);
                combo.setImmediate(true);
                combo.setNewItemsAllowed(true);
                combo.setInvalidAllowed(true);
                combo.setTextInputAllowed(true);
                combo.setScrollToSelectedItem(true);
                combo.setValue(record.getXpath());
                combo.addValueChangeListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        saveXPathSettings();
                    }
                });
                combo.addFocusListener(new FocusListener() {
                    public void focus(FocusEvent event) {
                        table.select(itemId);
                    }
                });
                field = combo;
            }
            return field;
        }
    }

    public class Record {
        ModelEntity modelEntity;

        ModelAttribute modelAttribute;

        String xpath = "";

        public Record(ModelEntity modelEntity, ModelAttribute modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;
            if (modelAttribute != null) {
                ComponentAttributeSetting setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                        XmlFormatter.XML_FORMATTER_XPATH);
                if (setting != null) {
                    xpath = setting.getValue();
                }
            } else {
                ComponentEntitySetting setting = component.getSingleEntitySetting(modelEntity.getId(), XmlFormatter.XML_FORMATTER_XPATH);
                if (setting != null) {
                    xpath = setting.getValue();
                }
            }
        }

        public int hashCode() {
            return modelEntity.hashCode() + (modelAttribute == null ? 0 : modelAttribute.hashCode());
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

        public String getEntityId() {
            return modelEntity.getId();
        }

        public String getAttributeName() {
            if (modelAttribute != null) {
                return modelAttribute.getName();
            }
            return null;
        }

        public String getAttributeId() {
            if (modelAttribute != null) {
                return modelAttribute.getId();
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
