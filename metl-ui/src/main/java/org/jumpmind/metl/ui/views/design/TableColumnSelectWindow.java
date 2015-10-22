/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.design;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.IConnectionCallback;
import org.jumpmind.db.sql.JdbcSqlTemplate;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.resource.Datasource;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Icons;
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

        List<Resource> list = context.getConfigurationService().findResourcesByTypes(model.getProjectVersionId(), Datasource.TYPE);
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
				a.setName(tableColumn.getName());
				a.setEntityId(e.getId());
				a.setType(tableColumn.getType());
				e.addModelAttribute(a);
			} else if (includeAllColumns) {
				tree.expandItem(table);
			    Collection<?> children = tree.getChildren(table);
			    for (Object object : children) {
	                TableColumn tableColumn = (TableColumn) object;
	                ModelAttribute a = new ModelAttribute();
	                a.setName(tableColumn.getName());
	                a.setEntityId(e.getId());
	                a.setType(tableColumn.getType());
	                e.addModelAttribute(a);
                }
			}
			tree.unselect(itemId);
		}
		return tableModelEntity.values();
	}
	
	protected void addCatalogsToResource(Resource resource) {
		Datasource dataSourceResource = (Datasource) context.getResourceFactory().create(resource, null);
		DataSource dataSource = (DataSource) dataSourceResource.reference();
		IDatabasePlatform platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource, new SqlTemplateSettings(), true, false);
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

    protected void addTablesToSchema(final Schema schema) {
        final IDatabasePlatform platform = platformByItemId.get(schema);
        JdbcSqlTemplate sqlTemplate = (JdbcSqlTemplate) platform.getSqlTemplate();
        sqlTemplate.execute(new IConnectionCallback<List<String>>() {
            public List<String> execute(Connection connection) throws SQLException {
                DatabaseMetaData meta = connection.getMetaData();
                ResultSet rs = null;
                try {
                    HashSet<String> tableTypes = new HashSet<String>();
                    rs = meta.getTableTypes();
                    while (rs.next()) {
                        tableTypes.add(rs.getString(1));
                    }
                    rs.close();

                    rs = meta.getTables(schema.getCatalog().getName(), schema.getName(), null,
                            tableTypes.toArray(new String[tableTypes.size()]));
                    while (rs.next()) {
                        addItem(new Table(schema, rs.getString(3)), platform, rs.getString(3),
                                FontAwesome.TABLE, schema, true);
                    }
                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                }
                return null;
            }
        });
    }

	protected void addColumnsToTable(final Table table) {
        final IDatabasePlatform platform = platformByItemId.get(table);
        JdbcSqlTemplate sqlTemplate = (JdbcSqlTemplate) platform.getSqlTemplate();
        sqlTemplate.execute(new IConnectionCallback<List<String>>() {
            public List<String> execute(Connection connection) throws SQLException {
                DatabaseMetaData meta = connection.getMetaData();
                ResultSet rs = null;
                try {
                    rs = meta.getColumns(table.getSchema().getCatalog().getName(), table.getSchema().getName(), table.getName(), null);
                    while (rs.next()) {
                        
                        DataType dataType = DataType.OTHER;
                        String type = rs.getString(6).toUpperCase();
                        try {
                            dataType = DataType.valueOf(type);
                        } catch (Exception ex) {
                            log.info("Could not map the data type of {} to a known datatype.", type);
                        }
                        addItem(new TableColumn(table, rs.getString(4), dataType.name()), platform, rs.getString(4),
                                FontAwesome.COLUMNS, table, true);
                    }
                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                }
                return null;
            }
        });
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
	
	class Table {
	    
	    Schema schema;
	    String name;
	    
        public Table(Schema schema, String name) {
            super();
            this.schema = schema;
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public Schema getSchema() {
            return schema;
        }

	}
	
	class TableColumn {
	    
	    String type;
	    
		String name;
		
		Table table;
		
		TableColumn(Table table, String name, String type) {
			this.table = table;
			this.name = name;
			this.type = type;
		}
		
		public Table getTable() {
			return table;
		}
		
		public String getName() {
            return name;
        }
		
		public String getType() {
            return type;
        }
		
	    public int hashCode() {
	        return table.hashCode() + name.hashCode() + type.hashCode();
	    }

	    public boolean equals(Object obj) {
	        if (obj instanceof TableColumn) {
	        	return hashCode() == ((TableColumn) obj).hashCode();
	        }
	        return false;
	    }
	}

}
