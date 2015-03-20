package org.jumpmind.symmetric.is.ui.views.design;

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
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Icons;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class TableColumnSelectWindow extends Window {
	
	private static final long serialVersionUID = 1L;

	ApplicationContext context;
	
	Tree tree = new Tree();
	
	Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();
	
	@SuppressWarnings({ "unchecked", "serial" })
	public TableColumnSelectWindow(ApplicationContext context) {
		this.context = context;
		
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
        
		setWidth(300.0f, Unit.PIXELS);
		setCaption("Hi Mom");
		setModal(true);
		VerticalLayout layout = new VerticalLayout();
		layout.addComponent(new Label("hi there"));
		layout.addComponent(tree);
		setContent(layout);		
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
			addItem(catalog, platform, catalog.getName(), Icons.DATABASE, resource, true);
		}
    }

	protected void addSchemasToCatalog(Catalog catalog) {
		IDatabasePlatform platform = platformByItemId.get(catalog);
		for (String schemaName : platform.getDdlReader().getSchemaNames(catalog.getName())) {
			Schema schema = new Schema(schemaName, catalog);
			addItem(schema, platform, schema.getName(), Icons.DATABASE, catalog, true);
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
			addItem(column, platform, column.getName(), FontAwesome.COLUMNS, table, false);
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

}