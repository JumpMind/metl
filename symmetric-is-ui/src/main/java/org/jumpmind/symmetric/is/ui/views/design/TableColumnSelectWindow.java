package org.jumpmind.symmetric.is.ui.views.design;

import java.util.List;

import javax.sql.DataSource;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Icons;

import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class TableColumnSelectWindow extends Window {
	
	ApplicationContext context;
	
	Tree tree = new Tree();
	
	public TableColumnSelectWindow(ApplicationContext context) {
		this.context = context;
		
		List<Resource> resources = context.getConfigurationService().findResourcesByTypes("Database");
		/*
		for (Resource resource : resources) {
			DataSourceResource dataSourceResource = (DataSourceResource) context.getResourceFactory().create(resource);
			DataSource dataSource = (DataSource) dataSourceResource.reference();
			try {
				dataSource.getConnection();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		*/
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
				} else if (itemId instanceof Schema) {
					
				} else if (itemId instanceof Catalog) {
					addSchemasToCatalog((Catalog) itemId);
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
        		tree.addItem(resource);
	            tree.getContainerProperty(resource, "name").setValue(resource.getName());
	            tree.setItemIcon(resource, Icons.DATABASE);
	            tree.setParent(resource, folder);
        	}
        }
    }
	
	protected void addCatalogsToResource(Resource resource) {
		DataSourceResource dataSourceResource = (DataSourceResource) context.getResourceFactory().create(resource);
		DataSource dataSource = (DataSource) dataSourceResource.reference();
		IDatabasePlatform platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource, new SqlTemplateSettings(), true);
		for (String catalogName : platform.getDdlReader().getCatalogNames()) {
			Catalog catalog = new Catalog(catalogName, resource); 
    		tree.addItem(catalog);
            tree.getContainerProperty(catalog, "name").setValue(catalogName);
            tree.setItemIcon(catalog, Icons.DATABASE);
            tree.setParent(catalog, resource);
		}
    }

	protected void addSchemasToCatalog(Catalog catalog) {
		DataSourceResource dataSourceResource = (DataSourceResource) context.getResourceFactory().create(catalog.getResource());
		DataSource dataSource = (DataSource) dataSourceResource.reference();
		IDatabasePlatform platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource, new SqlTemplateSettings(), true);
		for (String schemaName : platform.getDdlReader().getSchemaNames(catalog.getName())) {
			Schema schema = new Schema(schemaName, catalog.getResource()); 
    		tree.addItem(schema);
            tree.getContainerProperty(schema, "name").setValue(schemaName);
            tree.setItemIcon(schema, Icons.DATABASE);
            tree.setParent(schema, catalog);
		}
    }

	class Catalog extends AbstractObject {
		
		private static final long serialVersionUID = 1L;

		Resource resource;
		
		String name;
		
		public Catalog(String name, Resource resource) {
			this.name = name;
			this.resource = resource;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
		
		public Resource getResource() {
			return resource;
		}
	}
	
	class Schema extends Catalog {

		private static final long serialVersionUID = 1L;
		
		public Schema(String name, Resource resource) {
			super(name, resource);
		}
	}

}

