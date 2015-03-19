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
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
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

		ButtonBar buttonBar = new ButtonBar();
		addComponent(buttonBar);

		Button addEntityButton = buttonBar.addButton("Add Entity", FontAwesome.TABLE);
		addEntityButton.addClickListener(new AddEntityClickListener());
		
		Button addAttributeButton = buttonBar.addButton("Add Attribute", FontAwesome.COLUMNS);
		addAttributeButton.addClickListener(new AddAttributeClickListener());

		Button editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
		editButton.addClickListener(new EditClickListener());
		
		Button removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
		removeButton.addClickListener(new RemoveClickListener());

		Button importButton = buttonBar.addButton("Import ...", FontAwesome.DOWNLOAD);
		importButton.addClickListener(new ImportClickListener());

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
        	for (ModelAttribute a : e.getModelAttributes()) {
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
				if (itemId instanceof ModelAttribute) {
					context.getConfigurationService().delete((ModelAttribute) itemId);
				}
				treeTable.removeItem(itemId);
			}
			for (Object itemId : itemIds) {
				if (itemId instanceof ModelEntity) {
					context.getConfigurationService().delete((ModelEntity) itemId);
				}
				treeTable.removeItem(itemId);
			}
		}
	}

	class ImportClickListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
		}
	}

	class TreeTableValueChangeListener implements ValueChangeListener {
		public void valueChange(ValueChangeEvent event) {
			for (Object itemId : lastEditItemIds) {
				if (itemId instanceof ModelEntity) {
					ModelEntity e = (ModelEntity) itemId;
					e.setName((String) treeTable.getContainerProperty(itemId, "name").getValue());
					context.getConfigurationService().save(e);
				} else if (itemId instanceof ModelAttribute) {
					ModelAttribute a = (ModelAttribute) itemId;
					a.setName((String) treeTable.getContainerProperty(itemId, "name").getValue());
					a.setType((String) treeTable.getContainerProperty(itemId, "type").getValue());
					context.getConfigurationService().save(a);
				}
			}
	        treeTable.setEditable(false);
		}		
	}

}
