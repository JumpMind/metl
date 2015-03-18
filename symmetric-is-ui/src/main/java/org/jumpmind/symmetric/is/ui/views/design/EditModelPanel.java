package org.jumpmind.symmetric.is.ui.views.design;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.DataType;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.ModelVersion;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditModelPanel extends VerticalLayout implements IUiPanel {

	ApplicationContext context;
	
	TreeTable treeTable = new TreeTable();
	
	ModelVersion modelVersion;

	Set<Object> lastEditItemIds = Collections.emptySet();
	
	public EditModelPanel(ApplicationContext context, ModelVersion modelVersion) {
		this.context = context;
		this.modelVersion = modelVersion;
		
		Button addEntityButton = new Button("Add Entity");
		addEntityButton.setIcon(FontAwesome.TABLE);
		addEntityButton.setStyleName("button-bar");
		
		Button addAttributeButton = new Button("Add Attribute");
		addAttributeButton.setIcon(FontAwesome.COLUMNS);
		addAttributeButton.setStyleName("button-bar");
		
		Button editButton = new Button("Edit");
		editButton.setWidth("80px");
		editButton.setIcon(FontAwesome.EDIT);
		editButton.setStyleName("button-bar");

		Button removeButton = new Button("Remove");
		removeButton.setIcon(FontAwesome.TRASH_O);
		removeButton.setStyleName("button-bar");
		
		Button importButton = new Button("Import ...");
		importButton.setStyleName("button-bar");
		importButton.setIcon(FontAwesome.DOWNLOAD);

		addEntityButton.addClickListener(new AddEntityClickListener());
		addAttributeButton.addClickListener(new AddAttributeClickListener());
		removeButton.addClickListener(new RemoveClickListener());
		editButton.addClickListener(new EditClickListener());
		
		HorizontalLayout header = new HorizontalLayout();
		header.setWidth("100%");
		header.setMargin(new MarginInfo(true, false, true, false));
		HorizontalLayout bar = new HorizontalLayout();
		bar.setStyleName("button-bar");
		bar.addComponent(addEntityButton);
		bar.addComponent(addAttributeButton);
		bar.addComponent(editButton);
		bar.addComponent(removeButton);
		bar.addComponent(importButton);
		Label barSpacer = new Label();
		bar.addComponent(barSpacer);
		bar.setWidth("100%");
		bar.setExpandRatio(barSpacer, 1.0f);
		header.addComponent(bar);
		addComponent(header);

		treeTable.setSizeFull();
		treeTable.setCacheRate(100);
		treeTable.setPageLength(100);
		treeTable.setImmediate(true);
        treeTable.setSelectable(true);
        treeTable.setMultiSelect(true);
        
        treeTable.addContainerProperty("name", String.class, "", "Name", null, null);
        treeTable.addContainerProperty("type", String.class, "", "Type", null, null);
        treeTable.setVisibleColumns(new Object[] { "name", "type" });        
        treeTable.addValueChangeListener(new TreeTableValueChangeListener());
		addComponent(treeTable);
		setExpandRatio(treeTable, 1.0f);

        for (ModelEntity e : modelVersion.getModelEntities().values()) {
        	addModelEntity(e);
        	for (ModelAttribute a : e.getModelAttributes().values()) {
        		a.setEntity(e);
        		addModelAttribute(a);
        	}
        	treeTable.setCollapsed(e, false);
        }
	}
	
	@Override
	public boolean closing() {
		return true;
	}

	@Override
	public void showing() {
	}

	@SuppressWarnings("unchecked")
	protected Set<Object> getSelectedItems() {
		return (Set<Object>) treeTable.getValue();
	}
	
	protected void selectOnly(Object itemId) {
        for (Object id: getSelectedItems()) {
        	treeTable.unselect(id);
        }
        treeTable.select(itemId);
	}

	@SuppressWarnings("unchecked")
	protected void addModelEntity(ModelEntity modelEntity) {
		treeTable.addItem(modelEntity);
		treeTable.getContainerProperty(modelEntity, "name").setValue(modelEntity.getName());
		treeTable.setItemIcon(modelEntity, FontAwesome.TABLE);
		treeTable.setChildrenAllowed(modelEntity, true);
	}

	@SuppressWarnings("unchecked")
	protected void addModelAttribute(ModelAttribute modelAttribute) {
		treeTable.addItem(modelAttribute);
		treeTable.getContainerProperty(modelAttribute, "name").setValue(modelAttribute.getName());
		treeTable.getContainerProperty(modelAttribute, "type").setValue(modelAttribute.getType().toString());
		treeTable.setItemIcon(modelAttribute, FontAwesome.COLUMNS);
		treeTable.setParent(modelAttribute, modelAttribute.getEntity());
		treeTable.setChildrenAllowed(modelAttribute, false);
	}

	protected void editSelectedItem() {
		lastEditItemIds = getSelectedItems();
        treeTable.setTableFieldFactory(new TableFieldFactory() {
			public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {
				if (lastEditItemIds.contains(itemId)) {
					if (propertyId.equals("name")) {
						TextField t = new TextField();
						t.focus();
						t.selectAll();
						return t;
					} else if (propertyId.equals("type") && itemId instanceof ModelAttribute) {
						ComboBox cbox = new ComboBox();
						cbox.setNullSelectionAllowed(false);
						for (DataType dataType : DataType.values()) {
							cbox.addItem(dataType.toString());	
						}
						return cbox;
					}
				}
				return null;
			}
        });
        treeTable.setEditable(true);
	}

	class AddEntityClickListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
	        ModelEntity e = new ModelEntity();
	        e.setName("New Entity");
	        e.setModelVersionId(modelVersion.getId());
	        context.getConfigurationService().save(e);
	        addModelEntity(e);
	        selectOnly(e);
	        editSelectedItem();
		}
	}

	class AddAttributeClickListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
			Set<Object> itemIds = getSelectedItems();
			if (itemIds.size() > 0) {
		        ModelAttribute a = new ModelAttribute();
		        a.setName("New Attribute");
		        a.setDataType(DataType.STRING);
				Object itemId = itemIds.iterator().next();
				if (itemId instanceof ModelEntity) {
					a.setEntity((ModelEntity) itemId);
				} else if (itemId instanceof ModelAttribute) {
					a.setEntity(((ModelAttribute) itemId).getEntity());					
				}
				context.getConfigurationService().save(a);
		        addModelAttribute(a);
		        treeTable.setCollapsed(a.getEntity(), false);
		        selectOnly(a);
		        editSelectedItem();		        
			}
		}
	}

	class EditClickListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
			editSelectedItem();
		}
	}

	class RemoveClickListener implements ClickListener {
		@SuppressWarnings("unchecked")
		public void buttonClick(ClickEvent event) {
			Set<Object> itemIds = new HashSet<Object>();
			Set<Object> selectedIds = getSelectedItems();
			
			for (Object itemId : selectedIds) {
				Collection<Object> children = (Collection<Object>) treeTable.getContainerDataSource().getChildren(itemId);
				if (children != null) {
					itemIds.addAll(children);
				}
				itemIds.add(itemId);
			}

			for (Object itemId : itemIds) {
				if (itemId instanceof ModelEntity) {
					context.getConfigurationService().delete((ModelEntity) itemId);
				} else if (itemId instanceof ModelAttribute) {
					context.getConfigurationService().delete((ModelAttribute) itemId);
				}
				treeTable.removeItem(itemId);
			}
		}
	}
	
	class TreeTableValueChangeListener implements ValueChangeListener {
		public void valueChange(ValueChangeEvent event) {
			for (Object itemId : lastEditItemIds) {
				if (itemId instanceof ModelEntity) {
					context.getConfigurationService().save((ModelEntity) itemId);
				} else if (itemId instanceof ModelAttribute) {
					context.getConfigurationService().save((ModelAttribute) itemId);
				}				
			}
	        treeTable.setEditable(false);
		}		
	}

}
