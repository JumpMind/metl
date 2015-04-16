package org.jumpmind.symmetric.is.ui.views.design;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.component.TransformHelper;
import org.jumpmind.symmetric.is.core.runtime.component.Transformer;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditTransformerPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Component component;

    Table table = new Table();

    BeanItemContainer<ComponentAttributeSetting> container = new BeanItemContainer<ComponentAttributeSetting>(
            ComponentAttributeSetting.class);

    public EditTransformerPanel(ApplicationContext context, Component c) {
        this.context = context;
        this.component = c;

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSizeFull();
        table.addGeneratedColumn("entityName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                ComponentAttributeSetting setting = (ComponentAttributeSetting) itemId;
                Model model = component.getInputModel();
                ModelAttribute attribute = model.getAttributeById(setting.getAttributeId());
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                return entity.getName();
            }
        });
        table.addGeneratedColumn("attributeName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                ComponentAttributeSetting setting = (ComponentAttributeSetting) itemId;
                Model model = component.getInputModel();
                ModelAttribute attribute = model.getAttributeById(setting.getAttributeId());
                return attribute.getName();
            }
        });
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "value" });
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Transform" });
        table.setColumnExpandRatio("value", 1);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        addComponent(table);
        setExpandRatio(table, 1.0f);

        if (component.getInputModel() != null) {

            List<ComponentAttributeSetting> componentAttributes = component.getAttributeSettings();

            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                for (ModelAttribute attr : entity.getModelAttributes()) {
                    boolean found = false;
                    for (ComponentAttributeSetting componentAttribute : componentAttributes) {
                        if (componentAttribute.getAttributeId().equals(attr.getId())
                                && componentAttribute.getName().equals(
                                        Transformer.TRANSFORM_EXPRESSION)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        componentAttributes.add(new ComponentAttributeSetting(attr.getId(),
                                component.getId(), Transformer.TRANSFORM_EXPRESSION, null));
                    }
                }
            }

            Collections.sort(componentAttributes, new Comparator<ComponentAttributeSetting>() {
                @Override
                public int compare(ComponentAttributeSetting o1, ComponentAttributeSetting o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            for (ComponentAttributeSetting componentAttribute : componentAttributes) {
                table.addItem(componentAttribute);
            }

        }
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

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            final ComponentAttributeSetting setting = (ComponentAttributeSetting) itemId;
            Field<?> field = null;

            if (propertyId.equals("value")) {
                final ComboBox combo = new ComboBox();
                combo.setWidth(100, Unit.PERCENTAGE);
                String[] functions = TransformHelper.getSignatures();
                for (String function : functions) {
                    combo.addItem(function);
                }
                combo.setPageLength(functions.length > 20 ? 20 : functions.length);
                if (setting.getValue() != null && !combo.getItemIds().contains(setting.getValue())) {
                    combo.addItem(setting.getValue());
                }
                combo.setImmediate(true);
                combo.setNewItemsAllowed(true);
                combo.addValueChangeListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        setting.setValue((String) combo.getValue());
                        context.getConfigurationService().save(setting);
                    }
                });
                field = combo;
            }
            return field;
        }
    }

}
