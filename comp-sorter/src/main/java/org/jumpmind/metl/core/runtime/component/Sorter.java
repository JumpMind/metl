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
package org.jumpmind.metl.core.runtime.component;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.ObjectUtils;
import org.h2.Driver;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.IIndex;
import org.jumpmind.db.model.IndexColumn;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.DataType;

public class Sorter extends AbstractComponentRuntime {

    public static final String TYPE = "Sorter";

    public final static String SORT_ATTRIBUTE = "sort.attribute";
    
    public final static String SORTER_ATTRIBUTE_ORDINAL = "sort.attribute.order";

    public final static String ATTRIBUTE_SORTER_ENABLED = "sort.enabled";

    int rowsPerMessage;

    IDatabasePlatform databasePlatform;

    RdbmsWriter databaseWriter;
    
    String sortAttributeId;

    ArrayList<ComponentAttribSetting> sortKeyAttributeIdList = new ArrayList<>();
    
    List<EntityData> sortedRecords = new ArrayList<EntityData>();
    
    List<ModelEntity> entities;

    String databaseName;

    Throwable error;
    
    boolean entitySort = true;
    
    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);
        String sortAttribute = properties.get(SORT_ATTRIBUTE);
        Model inputModel = this.getComponent().getInputModel();
        Component component = context.getFlowStep().getComponent();
    	entities = new ArrayList<>(inputModel.getModelEntities());

        // check if a value was input on the main screen and if not verify there were sort attributes
        // selected in the component editor screen
        if (sortAttribute == null || sortAttribute.isEmpty()) {
        	entitySort = false;
            for (ModelEntity entity : entities) {
                for (ModelAttrib attribute : entity.getModelAttributes()) {
                	ComponentAttribSetting matchColumnSetting = component.getSingleAttributeSetting(attribute.getId(),
                            Sorter.SORTER_ATTRIBUTE_ORDINAL);
                    int matchColumn = matchColumnSetting != null
                            ? Integer.parseInt(matchColumnSetting.getValue()) : 0;
                    if (matchColumn > 0) {
                		// fill the list of attribute ids to sort on
                    	sortKeyAttributeIdList.add(matchColumnSetting);
                	}
		        }
	        }
        } else {
	        String[] joinAttributeElements = sortAttribute.split("[.]");
	        if (joinAttributeElements.length != 2) {
	            throw new IllegalStateException(
	                    "The sort attribute must be specified as 'entity.attribute'");
	        }
	        sortAttributeId = inputModel.getAttributeByName(joinAttributeElements[0],
	                joinAttributeElements[1]).getId();
	        ComponentAttribSetting attributeComponent = new ComponentAttribSetting(sortAttributeId, Sorter.SORTER_ATTRIBUTE_ORDINAL, "1");
	    	sortKeyAttributeIdList.add(attributeComponent);
	    }

        Collections.sort(sortKeyAttributeIdList, new Comparator<ComponentAttribSetting>() {
            public int compare(ComponentAttribSetting o1, ComponentAttribSetting o2) {
                return new Integer(Integer.parseInt(o1.getValue())==0?999999:Integer.parseInt(o1.getValue())).compareTo
                		(new Integer(Integer.parseInt(o2.getValue())==0?999999:Integer.parseInt(o2.getValue())));
            }
        });
        
        if (sortKeyAttributeIdList.size() == 0) {
        	throw new IllegalStateException(
        			"Sort attribute must be a valid 'entity.attribute' in the input model. "
        			+ "Or at least one attribute must be specified to sort on in the component editor.");
        }        
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	if (inputMessage instanceof EntityDataMessage) {
            if (!entitySort) {
	            createDatabase();
	            loadIntoDatabase(inputMessage);
            }
            
            ArrayList<EntityData> payload = ((EntityDataMessage)inputMessage).getPayload();
            for (int i = 0; i < payload.size(); i++) {
                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                
                if (entitySort) {
                	EntityData record = payload.get(i);
                	sortedRecords.add(record);
                }
            }
    	}

    	if (unitOfWorkBoundaryReached && error == null) {
    		if (entitySort) {
    			ArrayList<EntityData> dataToSend = new ArrayList<EntityData>();
    			
    			sort();

    			for (EntityData record : sortedRecords) {
    				if (dataToSend.size() >= rowsPerMessage) {
    					callback.sendEntityDataMessage(null, dataToSend);
    					dataToSend = new ArrayList<EntityData>();
    				}
    				dataToSend.add(record);
    			}

    			sortedRecords.clear();

    			if (dataToSend != null && dataToSend.size() > 0) {
    				callback.sendEntityDataMessage(null, dataToSend);
    			}
    		} else {
    			sortData(callback);
    		}
    	}
    }

    private void sort() {
    	Collections.sort(sortedRecords, new Comparator<EntityData>() {
    		@Override
    		public int compare(EntityData o1, EntityData o2) {
    			Object obj1 = o1.get(sortAttributeId);
    			Object obj2 = o2.get(sortAttributeId);
    			if ((obj1 instanceof Comparable || obj1 == null)
    					&& (obj2 instanceof Comparable || obj2 == null)) {
    				return ObjectUtils.compare((Comparable<?>) obj1, (Comparable<?>) obj2);
    			} else {
    				String str1 = obj1 != null ? obj1.toString() : null;
    				String str2 = obj2 != null ? obj2.toString() : null;
    				return ObjectUtils.compare(str1, str2);
    			}
    		}
    	});
    }

	private void sortData(ISendMessageCallback callback) {
		Map<ModelEntity, String> sqls = new HashMap<>();
		boolean addOrderBy = false;
		for (ModelEntity entity : entities) {
			StringBuilder sql = new StringBuilder("select ");
			appendColumns(sql, entity);
			sql.append(" from " + entity.getName() + "_1 ");
			
			// check to see if any one attribute of the entity is flagged to sort by before adding the 'order by' clause 
			for (ComponentAttribSetting componentAttribute : sortKeyAttributeIdList) {
				for (ModelAttrib attribute : entity.getModelAttributes()) {
					if (componentAttribute.getAttributeId().equals(attribute.getId())) {
						addOrderBy = true;
						break;
					}
				}
				if (addOrderBy) {
					break;
				}
			}
			
			if (addOrderBy) {
				sql.append(" order by ");
				appendSortColumns(sql, entity);
			}
			
			log(LogLevel.INFO, "Generated sort sql: %s", sql);
			sqls.put(entity, sql.toString());
			addOrderBy = false;
		}
		
		RdbmsReader reader = new RdbmsReader();
		reader.setDataSource(databasePlatform.getDataSource());
		reader.setContext(context);
		reader.setComponentDefinition(componentDefinition);
		reader.setRowsPerMessage(rowsPerMessage);
		reader.setThreadNumber(threadNumber);
		
		for (ModelEntity entity : entities) {
			reader.setSql(sqls.get(entity));
			reader.handle(new ControlMessage(this.context.getFlowStep().getId()), callback, false);
			info("Read %d  records for %s", reader.getRowReadDuringHandle(), entity.getName());
		}
		
		ResettableBasicDataSource ds = databasePlatform.getDataSource();
		
		ds.close();
		
		databasePlatform = null;
		databaseName = null;
		databaseWriter = null;
	}

	
	protected void appendColumns(StringBuilder sql, ModelEntity entity) {
		for (ModelAttrib attribute : entity.getModelAttributes()) {
			sql.append(attribute.getName()).append(" /* ")
				.append(entity.getName()).append(".").append(attribute.getName())
				.append(" */").append(",");
		}
		sql.replace(sql.length() - 1, sql.length(), "");
	}


	protected void appendSortColumns(StringBuilder sql, ModelEntity entity) {
		for (ComponentAttribSetting componentAttribute : sortKeyAttributeIdList) {
			for (ModelAttrib attribute : entity.getModelAttributes()) {
				if (componentAttribute.getAttributeId().equals(attribute.getId())) {
					sql.append(attribute.getName()).append(",");
					break;
				}
			}
		}
		sql.replace(sql.length() - 1, sql.length(), "");
	}

	protected void loadIntoDatabase(Message message) {
		String tableSuffix = "_1";
		
		if (databaseWriter == null) {
			databaseWriter = new RdbmsWriter();
			databaseWriter.setDatabasePlatform(databasePlatform);
			databaseWriter.setComponentDefinition(componentDefinition);
			databaseWriter.setReplaceRows(true);
			databaseWriter.setContext(context);
			databaseWriter.setThreadNumber(threadNumber);
		}
		
		if (tableSuffix != null) {
			databaseWriter.setTableSuffix(tableSuffix);
			try {
				databaseWriter.handle(message, null, false);
			} finally {
				error = databaseWriter.getError();
			}
		}
	}

	protected void createDatabase() {
		if (databasePlatform == null) {
			ResettableBasicDataSource ds = new ResettableBasicDataSource();
			ds.setDriverClassName(Driver.class.getName());
			ds.setMaxActive(1);
			ds.setInitialSize(1);
			ds.setMinIdle(1);
			ds.setMaxIdle(1);
			databaseName = UUID.randomUUID().toString();
			ds.setUrl("jdbc:h2:mem:" + databaseName);
			databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(ds,
					new SqlTemplateSettings(), true, false);
			
			Model inputModel = context.getFlowStep().getComponent().getInputModel();
			List<ModelEntity> entities = inputModel.getModelEntities();
			for (ModelEntity entity : entities) {
				Table table = new Table();
				table.setName(entity.getName() + "_1");
				List<ModelAttrib> attributes = entity.getModelAttributes();
				for (ModelAttrib attribute : attributes) {
					DataType dataType = attribute.getDataType();
					Column column = new Column(attribute.getName());
					if (dataType.isNumeric()) {
						column.setTypeCode(Types.DECIMAL);
					} else if (dataType.isBoolean()) {
						column.setTypeCode(Types.BOOLEAN);
					} else if (dataType.isTimestamp()) {
						column.setTypeCode(Types.TIMESTAMP);
					} else if (dataType.isBinary()) {
						column.setTypeCode(Types.BLOB);
					} else {
						column.setTypeCode(Types.LONGVARCHAR);
					}
					
					column.setPrimaryKey(attribute.isPk());
					table.addColumn(column);
				}
				
				alterCaseToMatchLogicalCase(table);
				databasePlatform.createTables(false, false, table);
			}
			
			log(LogLevel.INFO, "Creating databasePlatform with the following url: %s", ds.getUrl());
		}
	}


	private void alterCaseToMatchLogicalCase(Table table) {
		table.setName(table.getName().toUpperCase());
		
		Column[] columns = table.getColumns();
		for (Column column : columns) {
			column.setName(column.getName().toUpperCase());
		}
		
		IIndex[] indexes = table.getIndices();
		for (IIndex index : indexes) {
			index.setName(index.getName().toUpperCase());
			
			IndexColumn[] indexColumns = index.getColumns();
			for (IndexColumn indexColumn : indexColumns) {
				indexColumn.setName(indexColumn.getName().toUpperCase());
			}
		}
	}
}
