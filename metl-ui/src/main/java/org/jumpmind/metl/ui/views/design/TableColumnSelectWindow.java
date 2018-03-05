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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.DbProvider;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.jumpmind.vaadin.ui.sqlexplorer.DbTree;
import org.jumpmind.vaadin.ui.sqlexplorer.DefaultSettingsProvider;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import groovy.json.StringEscapeUtils;

public class TableColumnSelectWindow extends ResizableWindow implements ValueChangeListener, Receiver, SucceededListener {

    private static final long serialVersionUID = 1L;

    private static final String OPTION_DB = "Database";

    private static final String OPTION_REL_FILE = "Relational CSV File";
    
    private static final String OPTION_FILE_HEADER_ROW = "Source File Header Row";

    ApplicationContext context;

    Model model;

    DbTree dbTree;

    Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();

    TableColumnSelectListener listener;

    DbProvider provider;

    VerticalLayout optionLayout;
    
    OptionGroup optionGroup;
    
    Panel scrollable;

    Upload relCsvUpload;
    
    Upload fileHeaderUpload;
    
    TextField fileHeaderEntity;
    
    TextField fileHeaderDelimiter;

    ByteArrayOutputStream uploadedData;

    String delimiter = ",";

    String quoteCharacter = "\"";

    String encoding = "UTF-8";

    public TableColumnSelectWindow(ApplicationContext context, Model model) {
        super("Import Model Entity and Attributes");
        this.context = context;
        this.model = model;

        setWidth(600.0f, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setSizeFull();
        layout.addComponent(new Label("Import Entity and Attributes from a database, csv file or source file header row into the model."));

        optionGroup = new OptionGroup("Select the location of the model.");
        optionGroup.addItem(OPTION_DB);
        optionGroup.addItem(OPTION_REL_FILE);
        optionGroup.addItem(OPTION_FILE_HEADER_ROW);
        optionGroup.setNullSelectionAllowed(false);
        optionGroup.setImmediate(true);
        optionGroup.select(OPTION_DB);
        optionGroup.addValueChangeListener(this);
        layout.addComponent(optionGroup);

        optionLayout = new VerticalLayout();
        optionLayout.setSizeFull();
        
        scrollable = new Panel();
        scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
        scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        scrollable.setSizeFull();

        provider = new DbProvider(context);
        dbTree = new DbTree(provider, new DefaultSettingsProvider(context.getConfigDir()));
        scrollable.setContent(dbTree);

        relCsvUpload = new Upload("Comma separated file with 5 columns:  ENTITY, ATTRIBUTE, DESCRIPTION, DATA_TYPE, PK", this);
        relCsvUpload.addSucceededListener(this);
        relCsvUpload.setButtonCaption(null);

        fileHeaderEntity = new TextField("Entity Name");
        fileHeaderEntity.setColumns(25);
        fileHeaderEntity.setNullRepresentation("");
        fileHeaderEntity.setRequired(true);

        fileHeaderDelimiter = new TextField("Header Row Delimiter", ",");
        fileHeaderDelimiter.setColumns(5);
        fileHeaderDelimiter.setNullRepresentation("");
        fileHeaderDelimiter.setRequired(true);
        
        fileHeaderUpload = new Upload("Source file containing a header row to use as attributes (will be created as VARCHAR type, no PK)", this);
        fileHeaderUpload.addSucceededListener(this);
        fileHeaderUpload.setButtonCaption(null);

        layout.addComponent(optionLayout);
        layout.setExpandRatio(optionLayout, 1.0f);
        rebuildOptionLayout();
        addComponent(layout, 1);

        Button refreshButton = new Button("Refresh");
        Button cancelButton = new Button("Cancel");
        Button selectButton = new Button("Import");
        addComponent(buildButtonFooter(refreshButton, cancelButton, selectButton));

        cancelButton.addClickListener(event -> close());
        selectButton.addClickListener(event -> select());
        refreshButton.addClickListener(event -> refresh());
    }

    protected void rebuildOptionLayout() {
        optionLayout.removeAllComponents();
        if (optionGroup.getValue().equals(OPTION_DB)) {
            optionLayout.addComponent(scrollable);
            scrollable.focus();
        } else if (optionGroup.getValue().equals(OPTION_REL_FILE)) {
        	optionLayout.addComponent(relCsvUpload);
            relCsvUpload.focus();
        } else if (optionGroup.getValue().equals(OPTION_FILE_HEADER_ROW)) {
        	optionLayout.addComponent(fileHeaderEntity);
        	optionLayout.setExpandRatio(fileHeaderEntity, 0.2f);
        	optionLayout.addComponent(fileHeaderDelimiter);
        	optionLayout.setExpandRatio(fileHeaderDelimiter, 0.2f);
        	optionLayout.addComponent(fileHeaderUpload);
        	optionLayout.setExpandRatio(fileHeaderUpload, 0.8f);
        	fileHeaderEntity.focus();
        }
    }

    @Override
    public void valueChange(ValueChangeEvent event) {
        rebuildOptionLayout();
    }

    @Override
    public void uploadSucceeded(SucceededEvent event) {
        try {
        	if (optionGroup.getValue().equals(OPTION_REL_FILE)) {
        		listener.selected(importRelationalCsvModel(new String(uploadedData.toByteArray())));
            } else if (optionGroup.getValue().equals(OPTION_FILE_HEADER_ROW)) {
            	listener.selected(importFileHeaderModel(new String(uploadedData.toByteArray())));
            }
		} catch (IOException e) {
			throw new IoException(e);
		}
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        return uploadedData = new ByteArrayOutputStream();
    }

    protected void refresh() {
        if (optionGroup.getValue().equals(OPTION_DB)) {
            provider.refresh(true);
            dbTree.refresh();
        }
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }
    
    protected void select() {
        if (optionGroup.getValue().equals(OPTION_DB)) {
        	listener.selected(getModelEntityCollection());
            close();
        } else if (optionGroup.getValue().equals(OPTION_REL_FILE)) {
            relCsvUpload.submitUpload();
        } else if (optionGroup.getValue().equals(OPTION_FILE_HEADER_ROW)) {
            fileHeaderUpload.submitUpload();
        }
    }

    protected Collection<ModelEntity> getModelEntityCollection() {
        Set<Table> tables = dbTree.getSelectedTables();
        List<ModelEntity> entities = new ArrayList<>();
        for (Table table : tables) {
            ModelEntity entity = new ModelEntity();
            entity.setModelId(model.getId());
            entity.setName(table.getName());

            Column[] columns = table.getColumns();
            for (Column column : columns) {
                ModelAttrib attribute = new ModelAttrib();
                attribute.setName(column.getName());
                attribute.setPk(column.isPrimaryKey());
                try {
                    attribute.setDataType(DataType.valueOf(column.getMappedType().toUpperCase()));
                } catch (Exception ex) {
                    attribute.setDataType(DataType.OTHER);
                }
                attribute.setEntityId(entity.getId());
                entity.addModelAttribute(attribute);
            }
            
            entities.add(entity);
        }
        return entities;
    }

    public void setTableColumnSelectListener(TableColumnSelectListener listener) {
        this.listener = listener;
    }

    protected Collection<ModelEntity> importRelationalCsvModel(String text) throws IOException  {
        List<ModelEntity> entities = new ArrayList<>();
        if (text != null) {

            CsvReader csvReader = new CsvReader(new ByteArrayInputStream(text.getBytes(Charset.forName(encoding))), Charset.forName(encoding));
            csvReader.setDelimiter(delimiter.charAt(0));
            csvReader.setTextQualifier(quoteCharacter.charAt(0));
            csvReader.setUseTextQualifier(true);
            
            String entityName = "";
            String previousEntityName = "";
        	String attributeName;
        	String description;
        	String dataType;
        	String isPk;
        	boolean firstRec = true;
        	ModelEntity entity = null;
            while (csvReader.readRecord()) {
            	if (csvReader.getColumnCount() != 5) {
            		throw new IllegalStateException("The model input file is not in a valid format.  Please verify the file contains 5 columns comma separated. Entity,Attribute,Description,Type,PK");
            	}
            	
            	if (csvReader.get(0) != null && isNotBlank(csvReader.get(0).toString())) {
            		entityName = csvReader.get(0).toString();
            		firstRec = false;
            	} else if (firstRec) {
            		throw new IllegalStateException("The model input file is missing the Entity Name.  Please verify the file is formatted properly. Entity,Attribute,Description,Type,PK");
            	}
            	
            	if (csvReader.get(1) == null || csvReader.get(1).toString() == "") {
            		throw new IllegalStateException("The model input file is missing an Attribute Name.  Please verify the file is formatted properly. Entity,Attribute,Description,Type,PK");
            	}
            	attributeName = csvReader.get(1).toString();
            	description = csvReader.get(2) != null ? csvReader.get(2).toString() : "";
            	dataType = csvReader.get(3) != null ? csvReader.get(3).toString() : "";
            	isPk = csvReader.get(4) != null ? csvReader.get(4).toString() : "";

            	if (dataType.contains("(")) {
            		dataType = dataType.substring(0,dataType.indexOf("("));
            	}
            	
            	if (!previousEntityName.equals(entityName)) {
                    entity = new ModelEntity();
                    entity.setModelId(model.getId());
                    entity.setName(entityName);
                    previousEntityName = entityName;
            	}
                
                ModelAttrib attribute = new ModelAttrib();
                attribute.setName(attributeName);
                attribute.setDescription(description);
                attribute.setPk(("Y".equalsIgnoreCase(isPk) || "1".equalsIgnoreCase(isPk) || "X".equalsIgnoreCase(isPk) || "YES".equalsIgnoreCase(isPk)) ? true : false);
                try {
                    attribute.setDataType(DataType.valueOf(dataType.toUpperCase()));
                } catch (Exception ex) {
                    attribute.setDataType(DataType.OTHER);
                }
                attribute.setEntityId(entity.getId());
                entity.addModelAttribute(attribute);
                entities.add(entity);
            }
        	
        }
        close();
        return entities;
    }
    
    protected Collection<ModelEntity> importFileHeaderModel(String text) throws IOException  {
        List<ModelEntity> entities = new ArrayList<>();
        if (isNotBlank(text)) {
        	String fileDelimiter = fileHeaderDelimiter.getValue();
        	if (!isNotBlank(fileDelimiter) && !" ".equals(fileDelimiter)) {
        		throw new IllegalStateException("Must provide the delimiter of your file.");
        	}

        	String entityName = fileHeaderEntity.getValue();
        	if (!isNotBlank(entityName)) {
        		throw new IllegalStateException("Please provide an Entity Name.");
        	}

        	CsvReader csvReader = new CsvReader(new ByteArrayInputStream(text.getBytes(Charset.forName(encoding))), Charset.forName(encoding));
        	if ("\\t".equals(fileDelimiter)) {
        		csvReader.setDelimiter('\t');
        	} else {            
        		csvReader.setDelimiter(fileDelimiter.charAt(0));
        	}
            csvReader.setTextQualifier(quoteCharacter.charAt(0));
            csvReader.setUseTextQualifier(true);

            ModelEntity entity = null;
            while (csvReader.readRecord()) {
                entity = new ModelEntity();
                entity.setModelId(model.getId());
                entity.setName(entityName);
                
                for (int i = 0; i < csvReader.getColumnCount(); i++) {
                    ModelAttrib attribute = new ModelAttrib();
                    attribute.setName(csvReader.get(i).toString());
                    attribute.setDescription("");
                    attribute.setPk(false);
                    attribute.setDataType(DataType.VARCHAR);
                    attribute.setEntityId(entity.getId());
                    entity.addModelAttribute(attribute);
                    entities.add(entity);
                }
                // only read first line
                break;
            }

        } else {
        	throw new IllegalStateException("Please select a valid source file.");
        }
        close();
        return entities;
    }
}
