package org.jumpmind.symmetric.is.ui.views.design;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
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

    BeanItemContainer<AttributeFormat> container = new BeanItemContainer<AttributeFormat>(
            AttributeFormat.class);

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
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "width", "startPos",
                "endPos", "transformText" });
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Width",
                "Start Position", "End Position", "Transform" });
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        table.setDragMode(TableDragMode.ROW);
        table.setDropHandler(new TableDropHandler());
        addComponent(table);
        setExpandRatio(table, 1.0f);

        for (ModelEntity e : component.getInputModel().getModelEntities().values()) {
            for (ModelAttribute a : e.getModelAttributes()) {
                table.addItem(new AttributeFormat(e.getName(), a.getName(), 10));
            }
        }
        calculatePositions();
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void showing() {
    }

    protected void calculatePositions() {
        int pos = 1;
        boolean needsRefreshed = false;
        for (AttributeFormat a : container.getItemIds()) {
            if (a.getStartPos() != pos) {
            a.setStartPos(pos);
            needsRefreshed = true;
            }
            long endPos = pos + a.getWidth();
            if (a.getEndPos() != endPos) {
                a.setEndPos(endPos);
                needsRefreshed = true;
            }
            
            pos += a.getWidth() + 1;
        }

        if (needsRefreshed) {   
            editables.clear();
            table.refreshRowCache();
        }
    }

    class MoveUpClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Object itemId = table.getValue();
            int index = container.indexOfId(itemId);
            if (index != 0) {
                container.removeItem(itemId);
                container.addItemAt(index - 1, itemId);
                calculatePositions();
            }
        }
    }

    class MoveDownClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Object itemId = table.getValue();
            Object nextItemId = container.nextItemId(itemId);
            if (nextItemId != null) {
                container.removeItem(itemId);
                container.addItemAfter(nextItemId, itemId);
                calculatePositions();
            }
        }
    }
    
    List<TextField> editables = new ArrayList<TextField>();
    int focusIndex;

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(Container container, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            if (propertyId.equals("width") || propertyId.equals("transformText")) {
                final AttributeFormat attributeFormat = (AttributeFormat) itemId;
                final TextField textField = new TextField();
                textField.setData(itemId);
                textField.setImmediate(true);
                textField.addValueChangeListener(new ValueChangeListener() {

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        calculatePositions();
                    }
                });
                 textField.addFocusListener(new FocusListener() {
                 public void focus(FocusEvent event) {
                     focusIndex = editables.indexOf(textField) + 1;
                     table.select(((TextField) event.getSource()).getData());
                 }
                 });
                editables.add(textField);
                if (editables.size()-1 == focusIndex) {
                    textField.focus();
                }
                return textField;
            }
            return null;
        }
    }

    class TableDropHandler implements DropHandler {
        public void drop(DragAndDropEvent event) {
            AbstractSelectTargetDetails targetDetails = (AbstractSelectTargetDetails) event
                    .getTargetDetails();
            AttributeFormat target = (AttributeFormat) targetDetails.getItemIdOver();
            if (target != null) {
                Transferable transferable = event.getTransferable();
                if (transferable.getSourceComponent() == table) {
                    AttributeFormat source = (AttributeFormat) transferable.getData("itemId");
                    int diff = container.indexOfId(source) - container.indexOfId(target);
                    if (diff != 0) {
                        if (diff > 0) {
                            target = container.prevItemId(target);
                        }
                        container.removeItem(source);
                        container.addItemAfter(target, source);
                        calculatePositions();
                    }
                }
            }
        }

        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }

    public class AttributeFormat {
        String entityName;

        String attributeName;

        long width;

        long startPos;

        long endPos;

        String transformText = "";

        public AttributeFormat(String entityName, String attributeName, long width) {
            this.entityName = entityName;
            this.attributeName = attributeName;
            this.width = width;
        }

        public int hashCode() {
            return entityName.hashCode() + attributeName.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof AttributeFormat) {
                return hashCode() == ((AttributeFormat) obj).hashCode();
            }
            return super.equals(obj);
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public void setAttributeName(String attributeName) {
            this.attributeName = attributeName;
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
    }
}
