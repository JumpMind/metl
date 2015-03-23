package org.jumpmind.symmetric.is.ui.views.design;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Icons;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class TableColumnSelectWindow extends Window {
	
	private static final long serialVersionUID = 1L;

	ApplicationContext context;
	
	Model model;
	
	Tree tree = new Tree();
	
	Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();
	
	TableColumnSelectListener listener;
	
	@SuppressWarnings({ "unchecked", "serial" })
	public TableColumnSelectWindow(ApplicationContext context, Model model) {
		this.context = context;
		this.model = model;
		
		tree.setWidth(100f, Unit.PERCENTAGE);
		tree.setMultiSelect(true);
        tree.addContainerProperty("name", String.class, "");

		for (Folder folder : context.getConfigurationService().findFolders(FolderType.DESIGN)) {
			tree.addItem(folder);
			tree.getContainerProperty(folder, "name").setValue(folder.getName());				
		}
		
		tree.setItemCaptionPropertyId("name");
        tree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        tree.addExpandListener(new ExpandListener() {
			public void nodeExpand(ExpandEvent event) {
				Object itemId = event.getItemId();
				if (itemId instanceof Folder) {
					addDatabasesToFolder((Folder) itemId);
				} else if (itemId instanceof Resource) {
					addCatalogsToResource((Resource) itemId);
				} else if (itemId instanceof Catalog) {
					addSchemasToCatalog((Catalog) itemId);
				} else if (itemId instanceof Schema) {
					addTablesToSchema((Schema) itemId);
				} else if (itemId instanceof Table) {
					addColumnsToTable((Table) itemId);
				}
			}	        	
        });
        
		setWidth(600.0f, Unit.PIXELS);
		setHeight(600.0f, Unit.PIXELS);
		setCaption("Import from Database into Model");
		setModal(true);
		
		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(true);
		layout.setMargin(true);
		layout.setSizeFull();
		layout.addComponent(new Label("Select tables and columns to import into the model."));
		layout.addComponent(tree);
		layout.setExpandRatio(tree, 1.0f);
		
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setSpacing(true);
		buttonLayout.setMargin(true);
		buttonLayout.setWidth(100f, Unit.PERCENTAGE);
		Label buttonSpacer = new Label();
		buttonLayout.addComponent(buttonSpacer);
		buttonLayout.setExpandRatio(buttonSpacer, 1.0f);
		Button cancelButton = new Button("Cancel");
		buttonLayout.addComponent(cancelButton);
		Button selectButton = new Button("Import");
		buttonLayout.addComponent(selectButton);
		layout.addComponent(buttonLayout);		
		setContent(layout);
		
		cancelButton.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				close();
			}
		});

		selectButton.addClickListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				Collection<ModelEntity> modelEntityCollection = getModelEntityCollection();
				listener.selected(modelEntityCollection);
				close();
			}
		});		
	}

	@SuppressWarnings("unchecked")
	protected Collection<ModelEntity> getModelEntityCollection() {
		Collection<Object> itemIds = (Collection<Object>) tree.getValue();
		HashMap<Table, ModelEntity> tableModelEntity = new HashMap<Table, ModelEntity>();

		for (Object itemId : itemIds) {					
			Table table = null;
			if (itemId instanceof Table) {
				table = (Table) itemId;
			} else if (itemId instanceof TableColumn) {
				table = ((TableColumn) itemId).getTable();
			} else {
				continue;
			}

			ModelEntity e = tableModelEntity.get(table);
			if (e == null) {
				e = new ModelEntity();
				e.setName(table.getName());
				e.setModelId(model.getId());
				tableModelEntity.put(table, e);							
			}

			if (itemId instanceof TableColumn) {
				TableColumn tableColumn = (TableColumn) itemId;
				ModelAttribute a = new ModelAttribute();
				a.setName(tableColumn.getColumn().getName());
				a.setEntity(e);
				a.setType(tableColumn.getColumn().getJdbcTypeName());
				e.addModelAttribute(a);
			}
			tree.unselect(itemId);
		}
		return tableModelEntity.values();
	}

	protected void addDatabasesToFolder(Folder folder) {
        List<Resource> resources = context.getConfigurationService().findResourcesInFolder(folder);
        for (Resource resource : resources) {
        	if (resource.getType().equals("Database")) {
        		addItem(resource, null, resource.getName(), Icons.DATABASE, folder, true);
        	}
        }
    }
	
	protected void addCatalogsToResource(Resource resource) {
		DataSourceResource dataSourceResource = (DataSourceResource) context.getResourceFactory().create(resource);
		DataSource dataSource = (DataSource) dataSourceResource.reference();
		IDatabasePlatform platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource, new SqlTemplateSettings(), true);
		for (String catalogName : platform.getDdlReader().getCatalogNames()) {
			Catalog catalog = new Catalog(catalogName);
			addItem(catalog, platform, catalog.getName(), FontAwesome.CUBES, resource, true);
		}
    }

	protected void addSchemasToCatalog(Catalog catalog) {
		IDatabasePlatform platform = platformByItemId.get(catalog);
		for (String schemaName : platform.getDdlReader().getSchemaNames(catalog.getName())) {
			Schema schema = new Schema(schemaName, catalog);
			addItem(schema, platform, schema.getName(), FontAwesome.CUBES, catalog, true);
		}
    }

	protected void addTablesToSchema(Schema schema) {
		IDatabasePlatform platform = platformByItemId.get(schema);
		Database database = platform.getDdlReader().readTables(schema.getCatalog().getName(), schema.getName(), null);
		for (Table table : database.getTables()) {
			addItem(table, platform, table.getName(), FontAwesome.TABLE, schema, true);
		}
    }

	protected void addColumnsToTable(Table table) {
		IDatabasePlatform platform = platformByItemId.get(table);
		for (Column column : table.getColumns()) {
			addItem(new TableColumn(table, column), platform, column.getName(), FontAwesome.COLUMNS, table, false);
		}
    }

	@SuppressWarnings("unchecked")
	protected void addItem(Object itemId, IDatabasePlatform platform, String name, FontAwesome icon, Object parent, boolean areChildrenAllowed) {
		if (platform != null) {
			platformByItemId.put(itemId, platform);
		}
		tree.addItem(itemId);
        tree.getContainerProperty(itemId, "name").setValue(name);
        tree.setItemIcon(itemId, icon);
        tree.setParent(itemId, parent);
        tree.setChildrenAllowed(itemId, areChildrenAllowed);
	}
	
	public void setTableColumnSelectListener(TableColumnSelectListener listener) {
		this.listener = listener;
	}

	class SubDatabase {
		String name;
	    
	    SubDatabase(String name) {
	    	this.name = name;
	    }

		public String getName() {
			return name;
		}
		
	    public int hashCode() {
	        return name.hashCode();
	    }
	    
	    public boolean equals(Object obj) {
	    	if (obj instanceof SubDatabase) {
	    		return name.equals(((SubDatabase) obj).name);
	    	}
	    	return super.equals(obj);
	    }
	}

	class Catalog extends SubDatabase {
	    Catalog(String name) {
	    	super(name);
	    }
	}

	class Schema extends SubDatabase {
	    Catalog catalog;
	    
	    Schema(String name, Catalog catalog) {
	    	super(name);
	    	this.catalog = catalog;
	    }

		public Catalog getCatalog() {
			return catalog;
		}
	}
	
	class TableColumn {
		Column column;
		
		Table table;
		
		TableColumn(Table table, Column column) {
			this.table = table;
			this.column = column;
		}
		
		public Table getTable() {
			return table;
		}
		
		public Column getColumn() {
			return column;
		}
		
	    public int hashCode() {
	        return table.hashCode() + column.hashCode();
	    }

	    public boolean equals(Object obj) {
	        if (obj instanceof TableColumn) {
	        	return hashCode() == ((TableColumn) obj).hashCode();
	        }
	        return false;
	    }
	}

}