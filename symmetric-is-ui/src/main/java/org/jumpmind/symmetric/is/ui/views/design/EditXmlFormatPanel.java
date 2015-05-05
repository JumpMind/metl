package org.jumpmind.symmetric.is.ui.views.design;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.component.DelimitedParser;
import org.jumpmind.symmetric.is.core.runtime.component.XmlFormatter;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.views.design.EditFormatPanel.RecordFormat;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditXmlFormatPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Component component;

    Table table = new Table();

    BeanItemContainer<Record> container = new BeanItemContainer<Record>(Record.class);

    public EditXmlFormatPanel(ApplicationContext context, Component component) {
        this.context = context;
        this.component = component;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button moveUpButton = buttonBar.addButton("Edit Template", FontAwesome.FILE_CODE_O);
        moveUpButton.addClickListener(new EditTemplateClickListener());

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
        
        Model model = component.getInputModel();
        if (component.getType().equals(DelimitedParser.TYPE)) {
            model = component.getOutputModel();
        }

        if (model != null) {
            model = context.getConfigurationService().findModel(model.getId());
            for (ModelEntity entity : model.getModelEntities()) {
                for (ModelAttribute attr : entity.getModelAttributes()) {
                    table.addItem(new Record(entity, attr));
                }
            }
        }
        saveXPathSettings();
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    protected void saveXPathSettings() {
        String attrName = XmlFormatter.XML_FORMATTER_XPATH;
        for (Record record : container.getItemIds()) {
            saveSetting(record.getAttributeId(), attrName, StringUtils.trimToNull(record.getXpath()));
        }
    }

    protected void saveSetting(String attributeId, String name, String value) {
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

    class EditTemplateClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            EditTemplateWindow window = new EditTemplateWindow();
            window.show();
        }
    }

    class EditTemplateWindow extends ResizableWindow {
        public EditTemplateWindow() {
            super("Edit XML Template");
            setWidth(800f, Unit.PIXELS);
            setHeight(600f, Unit.PIXELS);
            content.setMargin(true);
            
            AceEditor editor = new AceEditor();
            editor.setImmediate(true);
            editor.setMode(AceMode.xml);
            editor.setSizeFull();
            //String acePath = VaadinServlet.getCurrent().getServletContext().getContextPath() + "/ace";
            //editor.setThemePath(acePath);
            //editor.setModePath(acePath);
            //editor.setWorkerPath(acePath);
            editor.setHighlightActiveLine(true);
            editor.setShowPrintMargin(false);
            addComponent(editor);
            content.setExpandRatio(editor, 1.0f);
            
            addComponent(buildButtonFooter(buildCloseButton()));
        }
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            final Record record = (Record) itemId;
            Field<?> field = null;
            if (propertyId.equals("xpath")) {
                final ComboBox combo = new ComboBox();
                combo.setWidth(100, Unit.PERCENTAGE);
                combo.addItem("//something/some/thing");
                //combo.setPageLength(functions.length > 20 ? 20 : functions.length);
                //if (record.getXPath() != null && 
                //        !combo.getItemIds().contains(record.getXPath())) {
                //    combo.addItem(record.getXPath());
                //}
                combo.setImmediate(true);
                combo.setNewItemsAllowed(true);
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
            ComponentAttributeSetting setting = component.getSingleAttributeSetting(modelAttribute.getId(),
                    XmlFormatter.XML_FORMATTER_XPATH);
            if (setting != null) {
                xpath = setting.getValue();
            }
        }

        public int hashCode() {
            return modelEntity.hashCode() + modelAttribute.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof RecordFormat) {
                return hashCode() == ((RecordFormat) obj).hashCode();
            }
            return super.equals(obj);
        }

        public String getEntityName() {
            return modelEntity.getName();
        }

        public String getAttributeName() {
            return modelAttribute.getName();
        }

        public String getAttributeId() {
            return modelAttribute.getId();
        }

        public String getXpath() {
            return xpath;
        }

        public void setXpath(String xpath) {
            this.xpath = xpath;
        }
    }
}
