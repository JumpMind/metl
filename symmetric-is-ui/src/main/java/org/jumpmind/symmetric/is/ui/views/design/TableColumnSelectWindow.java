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
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.ui.common.ResizableWindow;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class TableColumnSelectWindow extends ResizableWindow {
	
	private static final long serialVersionUID = 1L;

	ApplicationContext context;
	
	Model model;
	
	Tree tree = new Tree();
	
	Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();
	
	TableColumnSelectListener listener;
	
	@SuppressWarnings({ "serial" })
	public TableColumnSelectWindow(ApplicationContext context, Model model) {
	    super("Import from Database into Model");
		this.context = context;
		this.model = model;
		
		//tree.setSizeFull();
		tree.setMultiSelect(true);
        tree.addContainerProperty("name", String.class, "");

        List<Resource> list = context.getConfigurationService().findResourcesByTypes(model.getProjectVersionId(), DataSourceResource.TYPE);
		for (Resource resource : list) {
		    addItem(resource, null, resource.getName(), Icons.DATABASE, null, true);
        }
		
		tree.setItemCaptionPropertyId("name");
        tree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        tree.addExpandListener(new ExpandListener() {
			public void nodeExpand(ExpandEvent event) {
				Object itemId = event.getItemId();
				if (itemId instanceof Resource) {
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
		
		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(true);
		layout.setMargin(true);
		layout.setSizeFull();
		layout.addComponent(new Label("Select tables and columns to import into the model."));
		
		Panel scrollable = new Panel();
		scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
		scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
		scrollable.setSizeFull();
		scrollable.setContent(tree);
		layout.addComponent(scrollable);
		layout.setExpandRatio(scrollable, 1.0f);
		addComponent(layout, 1);
		
		Button cancelButton = new Button("Cancel");
		Button selectButton = new Button("Import");
		addComponent(buildButtonFooter(cancelButton, selectButton));
		
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
            boolean includeAllColumns = true;
			Table table = null;
			if (itemId instanceof Table) {
				table = (Table) itemId;
				for (Object object : itemIds) {
                    if (object instanceof TableColumn) {
                        TableColumn column = (TableColumn)object;
                        if (column.getTable().equals(table)) {
                            includeAllColumns = false;
                        }
                    }
                }
				
			} else if (itemId instanceof TableColumn) {
				table = ((TableColumn) itemId).getTable();
				includeAllColumns = false;
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
				a.setEntityId(e.getId());
				a.setType(tableColumn.getColumn().getMappedType());
				e.addModelAttribute(a);
			} else if (includeAllColumns) {
			    Column[] columns = table.getColumns();
			    for (Column column : columns) {
			        ModelAttribute a = new ModelAttribute();
	                a.setName(column.getName());
	                a.setEntityId(e.getId());
	                a.setType(column.getMappedType());
	                e.addModelAttribute(a);
                }
			}
			tree.unselect(itemId);
		}
		return tableModelEntity.values();
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