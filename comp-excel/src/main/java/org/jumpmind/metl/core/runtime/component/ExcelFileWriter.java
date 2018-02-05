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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.properties.TypedProperties;

public class ExcelFileWriter extends AbstractFileWriter {

    public final static String TYPE = "Excel File Writer";

    public final static String SETTING_EMPTY_FILE = "empty.file";

    public final static String EXCEL_OUTPUT_TYPE = "excel.output.type";

    public final static String SETTING_SHEET_NAME = "sheet.name";

    public final static String SETTING_INCLUDE_HEADER = "include.header";

    public final static String EXCEL_WRITER_ATTRIBUTE_ORDINAL = "excel.writer.attribute.ordinal";

    public final static String EXCEL_OUTPUT_FORMAT = "Microsoft Excel XML (.xlsx)";
    
    boolean emptyFile;

    boolean inputDataReceived = false;
    
    boolean includeHeader;
            
    String excelOutputType = EXCEL_OUTPUT_TYPE;
    
    String excelSheetName = "Sheet1";

    OutputStream fileOut = null;

    IDirectory directory = null;
    
    Workbook wb = null;
    
    Sheet sheet = null;
    
    int rowNbr = 0;  // ie: row 1
    
    int colNbr = 0;  // ie: column A
    
    List<AttributeFormat> attributes = new ArrayList<AttributeFormat>();

    @Override
    public void start() {
        init();
        TypedProperties properties = getTypedProperties();
        emptyFile = properties.is(SETTING_EMPTY_FILE, false);
        includeHeader = properties.is(SETTING_INCLUDE_HEADER, false);
        excelOutputType = properties.get(EXCEL_OUTPUT_TYPE, excelOutputType);
        excelSheetName = properties.get(SETTING_SHEET_NAME, excelSheetName);
        String enteredFileName = properties.get(SETTING_RELATIVE_PATH);
        
        // make sure the user entered a valid name extension matching the type of output requested
        if (enteredFileName.endsWith(".xlsx") && "Microsoft Excel (.xls)".equals(excelOutputType)) {
        	throw new MisconfiguredException("The filename extension entered must match the selected Excel output type (.xls).");
        } else if (enteredFileName.endsWith(".xls") && "Microsoft Excel XML (.xlsx)".equals(excelOutputType)) {
        	throw new MisconfiguredException("The filename extension entered must match the selected Excel output type (.xlsx).");
        } else if (!enteredFileName.endsWith(".xls") && !enteredFileName.endsWith(".xlsx")) {
        	if ("Microsoft Excel (.xls)".equals(excelOutputType)) {
        		properties.setProperty(SETTING_RELATIVE_PATH, enteredFileName + ".xls");
        	} else {
        		properties.setProperty(SETTING_RELATIVE_PATH, enteredFileName + ".xlsx");
        	}
        	relativePathAndFile = properties.get(SETTING_RELATIVE_PATH);
        }
        
        if ("Microsoft Excel (.xls)".equals(excelOutputType)) {
        	wb = new HSSFWorkbook();
        } else {
            wb = new XSSFWorkbook();
        }
        
        // fix user entered sheet (tab) name if it does not meet the Excel requirements
        String safeName = WorkbookUtil.createSafeSheetName(excelSheetName);
        sheet = wb.createSheet(safeName);
        convertAttributeSettingsToAttributeFormat();
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("The resource has not been configured.  Please choose a resource.");
        }
        if (attributes.size() == 0) {
            log(LogLevel.INFO, "There is no attribute ordering configured.  Writing all entity fields to the output as defined in model");

        	Model inputModel = this.getComponent().getInputModel();
        	List<ModelEntity> entities = new ArrayList<>(inputModel.getModelEntities());
            Map<String, AttributeFormat> formats = new HashMap<String, AttributeFormat>();
            for (ModelEntity entity : entities) {
            	for (ModelAttrib attribute : entity.getModelAttributes()) {
            		AttributeFormat format = formats.get(attribute.getId());
	                if (format == null) {
                        format = new AttributeFormat(attribute.getId(), entity, attribute);
                        format.setOrdinal(attribute.getAttributeOrder());
                        formats.put(attribute.getId(), format);
	                }
            	}
            }
            
            attributes.addAll(formats.values());
            Collections.sort(attributes, new Comparator<AttributeFormat>() {
                public int compare(AttributeFormat ordinal1, AttributeFormat ordinal2) {
                    return ordinal1.getOrdinal() - ordinal2.getOrdinal();
                }
            });
        }

        if ((unitOfWorkBoundaryReached && inputDataReceived == false && emptyFile == true) || (inputMessage instanceof EntityDataMessage)) {
            inputDataReceived = true;
            
            // if we need to include the header add it to the output and update the boolean to not output again
            if (includeHeader) {
            	Row row = sheet.createRow((short)rowNbr);
        		for (AttributeFormat attr : attributes) {
                    if (attr.getAttribute() != null) {
                		Cell cell = row.createCell(colNbr);
                    	cell.setCellValue(attr.getAttribute() != null ? attr.getAttribute().getName() : "");
                    	colNbr++;
                    }
                }
            	
                rowNbr++;
                colNbr = 0;
            	includeHeader = false;
            }
            
            initStreamAndWriter(inputMessage);

            if (inputMessage instanceof EntityDataMessage) {
                ArrayList<EntityData> inputRows = ((EntityDataMessage)inputMessage).getPayload();
                for (EntityData inputRow : inputRows) {
                	Row row = sheet.createRow((short)rowNbr);
                	for (AttributeFormat attribute : attributes) {
                        Object object = inputRow.get(attribute.getAttributeId());
                    	Cell cell = row.createCell(colNbr);
                    	cell.setCellValue(object != null ? object.toString() : "");
                    	colNbr++;
                    }
                	rowNbr++;
                	colNbr = 0;
                }
            }
        }

    	try {
	        if ((inputMessage instanceof ControlMessage || unitOfWorkBoundaryReached) && callback != null) {
				if (fileOut != null) {
					wb.write(fileOut);
				}
	            closeDirectory();
	            ArrayList<String> results = new ArrayList<>(1);
	            results.add("{\"status\":\"success\"}");
	            callback.sendTextMessage(null, results);
	        } else if (inputMessage instanceof ContentMessage) {
				if (fileOut != null) {
					wb.write(fileOut);
				}
	            closeFile();
	        }
		} catch (IOException e) {
			throw new IoException(e);
		}
    }
    
    private void closeDirectory() {
        if (directory != null) {
            directory.close();
            directory = null;
        }        
    }
    
    @Override
    public void stop() {
        super.stop();
        closeDirectory();
    }

    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        closeFile();
        super.flowCompletedWithErrors(myError);
    }

    private void initStreamAndWriter(Message inputMessage) {
        if (fileOut == null) {
            String fileName = getFileName(inputMessage);
            if (directory == null) {
                directory = (IDirectory) getResourceReference();
            }

            directory.delete(fileName);

            if (isNotBlank(fileName)) {
                log(LogLevel.INFO, String.format("Writing Excel to resource: %s with name: %s", directory.toString(), fileName));
            } else {
                log(LogLevel.INFO, String.format("Writing Excel to resource: %s", directory.toString()));
            }
            fileOut = directory.getOutputStream(fileName, mustExist, false, append);        
        }
    }

    private void closeFile() {
        IOUtils.closeQuietly(fileOut);
        fileOut = null;
    }
    
    private void convertAttributeSettingsToAttributeFormat() {
        List<ComponentAttribSetting> attributeSettings = getComponent().getAttributeSettings();
        Map<String, AttributeFormat> formats = new HashMap<String, AttributeFormat>();
        for (ComponentAttribSetting attributeSetting : attributeSettings) {
            AttributeFormat format = formats.get(attributeSetting.getAttributeId());
            if (format == null) {
                Model inputModel = getComponent().getInputModel();
                ModelAttrib attribute = inputModel.getAttributeById(attributeSetting.getAttributeId());
                if (attribute != null) {
                    ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                    format = new AttributeFormat(attributeSetting.getAttributeId(), entity, attribute);
                    formats.put(attributeSetting.getAttributeId(), format);
                }
            }
            if (format != null) {
                if (attributeSetting.getName().equalsIgnoreCase(EXCEL_WRITER_ATTRIBUTE_ORDINAL)) {
                    format.setOrdinal(Integer.parseInt(attributeSetting.getValue()));
                }
            }
        }

        attributes.addAll(formats.values());
        Collections.sort(attributes, new Comparator<AttributeFormat>() {
            public int compare(AttributeFormat ordinal1, AttributeFormat ordinal2) {
                return ordinal1.getOrdinal() - ordinal2.getOrdinal();
            }
        });
    }

    private class AttributeFormat {

        ModelEntity entity;
        ModelAttrib attribute;
        String attributeId;
        int ordinal;

        public AttributeFormat(String attributeId, ModelEntity entity, ModelAttrib attribute) {
            this.attributeId = attributeId;
            this.entity = entity;
            this.attribute = attribute;
        }

        public String getAttributeId() {
            return attributeId;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        public ModelAttrib getAttribute() {
            return attribute;
        }

        public ModelEntity getEntity() {
            return entity;
        }
    }    
}
