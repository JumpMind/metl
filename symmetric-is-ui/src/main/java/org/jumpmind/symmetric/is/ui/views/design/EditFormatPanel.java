package org.jumpmind.symmetric.is.ui.views.design;

import java.util.HashMap;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.component.DelimitedFormatter;
import org.jumpmind.symmetric.is.core.runtime.component.FixedLengthFormatter;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditFormatPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Component component;

    Table table = new Table();

    BeanItemContainer<RecordFormat> container = new BeanItemContainer<RecordFormat>(RecordFormat.class);

    public EditFormatPanel(ApplicationContext context, Component component) {
        this.context = context;
        this.component = component;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button moveUpButton = buttonBar.addButton("Move Up", FontAwesome.ARROW_UP);
        moveUpButton.addClickListener(new MoveUpClickListener());

        Button moveDownButton = buttonBar.addButton("Move Down", FontAwesome.ARROW_DOWN);
        moveDownButton.addClickListener(new MoveDownClickListener());

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSizeFull();
        if (component.getType().equals(FixedLengthFormatter.TYPE)) {
	        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "width", "startPos", "endPos", "transformText" });
	        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Width", "Start Position", "End Position", "Transform" });
        } else {
	        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "transformText" });
	        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Transform" });      	
        }
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        table.setDragMode(TableDragMode.ROW);
        table.setDropHandler(new TableDropHandler());
        addComponent(table);
        setExpandRatio(table, 1.0f);

        if (component.getInputModel() != null) {
	        for (ModelEntity entity : component.getInputModel().getModelEntities()) {
	            for (ModelAttribute attr : entity.getModelAttributes()) {
	                table.addItem(new RecordFormat(entity, attr));
	            }
	        }
        }
        calculatePositions();
    	saveOrdinalSettings();
    	saveLengthSettings();
    	saveTransformSettings();
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void showing() {
    }

    protected void calculatePositions() {
    	if (component.getType().equals(DelimitedFormatter.TYPE)) {
    		return;
    	}
        int pos = 1;
        boolean needsRefreshed = false;
        for (RecordFormat record : container.getItemIds()) {
            if (record.getStartPos() != pos) {
            	record.setStartPos(pos);
	            needsRefreshed = true;
            }
            long endPos = pos + record.getWidth();
            if (record.getEndPos() != endPos) {
            	record.setEndPos(endPos);
                needsRefreshed = true;
            }
            pos += record.getWidth() + 1;
        }

        if (needsRefreshed) {
        	RecordFormat record = (RecordFormat) table.getValue();
        	if (record != null) {
        		record.setFocusFieldId("transformText");
        	}
            table.refreshRowCache();
        }
    }

    protected void moveItemTo(RecordFormat itemId, int index) {
    	if (index >= 0  && index < container.getItemIds().size() && itemId != null && index != container.indexOfId(itemId)) {
    		container.removeItem(itemId);
            container.addItemAt(index, itemId);
            calculatePositions();
            saveOrdinalSettings();
    	}
    }
    
    protected void saveOrdinalSettings() {
    	String attrName = FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_ORDINAL;
    	if (component.getType().equals(DelimitedFormatter.TYPE)) {
    		attrName = DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL;
    	}
        int ordinal = 1;
        for (RecordFormat record : container.getItemIds()) {
        	saveSetting(record.getAttributeId(), attrName, String.valueOf(ordinal));
			ordinal++;
        }
    }

    protected void saveLengthSettings() {
    	if (component.getType().equals(DelimitedFormatter.TYPE)) {
    		return;
    	}
    	for (RecordFormat record : container.getItemIds()) {
    		saveSetting(record.getAttributeId(), FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH, String.valueOf(record.getWidth()));
    	}
    }

    protected void saveTransformSettings() {
    	String attrName = FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION;
    	if (component.getType().equals(DelimitedFormatter.TYPE)) {
    		attrName = DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION;
    	}

    	for (RecordFormat record : container.getItemIds()) {
			saveSetting(record.getAttributeId(), attrName, record.getTransformText());
    	}
    }

    protected void saveSetting(String attributeId, String name, String value) {
		ComponentAttributeSetting setting = component.getAttributeSetting(attributeId, name);
		if (setting == null) {
			setting = new ComponentAttributeSetting(attributeId, name, value);
			setting.setComponentId(component.getId());
			component.addAttributeSetting(setting);
			context.getConfigurationService().save(setting);
		} else if (!setting.getValue().equals(value)) {
			setting.setValue(value);
			context.getConfigurationService().save(setting);
		}
    }

    class MoveUpClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        	RecordFormat itemId = (RecordFormat) table.getValue();
        	moveItemTo(itemId, container.indexOfId(itemId) - 1);
        }
    }

    class MoveDownClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        	RecordFormat itemId = (RecordFormat) table.getValue();
        	moveItemTo(itemId, container.indexOfId(itemId) + 1);
        }
    }
    
    class TableDropHandler implements DropHandler {
        public void drop(DragAndDropEvent event) {
            AbstractSelectTargetDetails targetDetails = (AbstractSelectTargetDetails) event
                    .getTargetDetails();
            RecordFormat target = (RecordFormat) targetDetails.getItemIdOver();
            Transferable transferable = event.getTransferable();
            if (transferable.getSourceComponent() == table) {
                RecordFormat source = (RecordFormat) transferable.getData("itemId");
                moveItemTo(source, container.indexOfId(target));
            }
        }

        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }

	class EditFieldFactory implements TableFieldFactory {
		public Field<?> createField(final Container dataContainer, final Object itemId,
				final Object propertyId, com.vaadin.ui.Component uiContext) {
            final RecordFormat record = (RecordFormat) itemId;
            Field<?> field = null;
			if (propertyId.equals("width")) {
				final TextField textField = new TextField();
				textField.setImmediate(true);
				textField.addValueChangeListener(new ValueChangeListener() {
					public void valueChange(ValueChangeEvent event) {
						calculatePositions();
						saveLengthSettings();
					}
				});
				textField.addFocusListener(new FocusListener() {
					public void focus(FocusEvent event) {
						record.setFocusFieldId(propertyId);
						table.select(itemId);
					}
				});
				record.addField(propertyId, textField);
				field = textField;
			} else if (propertyId.equals("transformText")) {
				final ComboBox combo = new ComboBox();
				combo.addItem("format(spec)");
				combo.addItem("left(length)");
				combo.addItem("lower()");
				combo.addItem("pad(char, length)");
				combo.addItem("right(length)");
				combo.addItem("substr(start, end)");
				combo.addItem("trim()");
				combo.addItem("upper()");
				combo.setImmediate(true);
                combo.addValueChangeListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                    	saveTransformSettings();
                    }
                });
                combo.addFocusListener(new FocusListener() {
					public void focus(FocusEvent event) {
						record.setFocusFieldId(propertyId);
						table.select(itemId);
					}
				});
                record.addField(propertyId, combo);
                field = combo;
			}

			RecordFormat selected = (RecordFormat) table.getValue();
			if (selected == itemId && record.getFocusField() != null) {
				record.getFocusField().focus();
			}
			return field;
		}
	}

    public class RecordFormat {
    	ModelEntity modelEntity;
    	
    	ModelAttribute modelAttribute;
    	
    	Map<Object, Field<?>> fields = new HashMap<Object, Field<?>>();

    	Object focusFieldId;

        long width = 1;

        long startPos;

        long endPos;

        String transformText = "";

        public RecordFormat(ModelEntity modelEntity, ModelAttribute modelAttribute) {
            this.modelEntity = modelEntity;
            this.modelAttribute = modelAttribute;
            ComponentAttributeSetting setting = component.getAttributeSetting(modelAttribute.getId(), 
            		FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH);
            if (setting != null) {
            	this.width = Long.parseLong(setting.getValue());
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

        public long getWidth() {
            return width;
        }

        public void setWidth(long width) {
            this.width = width;
        }

        public String getTransformText() {
            return transformText;
        }

        public void setTransformText(String transformText) {
            this.transformText = transformText;
        }

        public long getStartPos() {
            return startPos;
        }

        public void setStartPos(long startPos) {
            this.startPos = startPos;
        }

        public long getEndPos() {
            return endPos;
        }

        public void setEndPos(long endPos) {
            this.endPos = endPos;
        }

        public void addField(Object id, Field<?> field) {
        	fields.put(id, field);
        }
        
        public void setFocusFieldId(Object id) {
        	this.focusFieldId = id;
        }
        
        public Field<?> getFocusField() {
        	Field<?> field = fields.get(focusFieldId);
        	if (field == null) {
        		field = fields.get("width");
        	}
        	return field;
        }
    }
}
