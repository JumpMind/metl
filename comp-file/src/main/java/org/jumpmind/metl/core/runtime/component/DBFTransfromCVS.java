
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jamel.dbf.DbfReader;
import org.jamel.dbf.exception.DbfException;
import org.jamel.dbf.structure.DbfField;
import org.jamel.dbf.structure.DbfHeader;
import org.jamel.dbf.utils.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.LocalFileDirectory;
import org.jumpmind.properties.TypedProperties;

public class DBFTransfromCVS extends AbstractFileReader {

    public static final String TYPE = "DBF Transfrom";

    public static final String SETTING_ROWS_PER_MESSAGE = "text.rows.per.message";

    public static final String SETTING_ENCODING = "encoding";

    public static final String SETTING_HEADER_LINES_TO_SKIP = "text.header.lines.to.skip";

    public static final String SETTING_NUMBER_OF_TIMES_TO_READ_FILE = "number.of.times.to.read.file";

    public static final String SETTING_SPLIT_ON_LINE_FEED = "split.on.line.feed";

    int textRowsPerMessage = 1000;

    int numberOfTimesToReadFile = 1;

    int textHeaderLinesToSkip;

    String encoding = "UTF-8";
    
    String delimit = "#";
    

    @Override
    public void start() {
        init();
        TypedProperties properties = getTypedProperties();
       
        textHeaderLinesToSkip = properties.getInt(SETTING_HEADER_LINES_TO_SKIP, textHeaderLinesToSkip);
        textRowsPerMessage = properties.getInt(SETTING_ROWS_PER_MESSAGE, textRowsPerMessage);
        numberOfTimesToReadFile = properties.getInt(SETTING_NUMBER_OF_TIMES_TO_READ_FILE, numberOfTimesToReadFile);
        encoding = properties.get(SETTING_ENCODING, encoding);
        if ("".equals(encoding)) {
        	encoding = "UTF-8";
        	log(LogLevel.INFO, "File Encoding has not been set, using the default of UTF-8.");
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
                || (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
        	// 读取文件情况信息
        	System.out.println("dbf"+inputMessage);
        	List<String> files = getFilesToRead(inputMessage);
        	System.out.println("dbf"+files.get(0));
            processFiles(files, inputMessage, callback, unitOfWorkBoundaryReached);
        }
    }

    private void processFiles(List<String> files, Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
        int linesInMessage = 0;
        ArrayList<String> payload = new ArrayList<String>();

        filesRead.addAll(files);

        for (String file : files) {
            Map<String, Serializable> headers = new HashMap<>(1);
            headers.put("source.file.path", file);
            int currentFileLinesRead = 0;
            String currentLine;
            boolean readContent = true;
            if (directory == null) {
                throw new IllegalStateException("The resource was not created.  Please check to see that it is properly configured");
            }
            try {
                for (int i = 0; i < numberOfTimesToReadFile && readContent; i++) {
                    checkForInterruption();
                    if (isNotBlank(file)) {
                        info("Reading file: %s", file);
                    }
                    String filePath = resolveParamsAndHeaders(file, inputMessage);
                    BufferedReader reader = null;
                    try {
                        InputStream inStream = directory.getInputStream(filePath, mustExist);
                        if (inStream != null) {
               
                        	String DBFName =files.get(0).toString();
                        	String CSVName=DBFName.substring(0, DBFName.length()-4)+".csv";                        	
                        	writeToTxtFile(new File(DBFName),new File(CSVName),Charset.forName("gbk"));
                        	//删除文件
                        	//deleteFile(fileName); 	
                        	
                    } else {
                            if (isNotBlank(file)) {
                                info("File %s didn't exist, but must exist setting was false.  Continuing", file);
                            } else {
                                info("There was no content to read");
                            }
                            readContent = false;
                        }
                    } finally {
                        // Closes the reader and the inStream.
                        IOUtils.closeQuietly(reader);
                    }
                }
            } catch (Exception e) {
                throw new IoException("Error reading from file " + e.getMessage());
            }

            if (controlMessageOnEof) {
                callback.sendControlMessage(headers);
            }
        }
    }
    
    public static void writeToTxtFile(File dbf,File csv, Charset dbfEncoding) {
    	String delimit="#";
        try {
            DbfReader reader = new DbfReader(dbf);
            
           // PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(csv)));
           
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(csv)));
            DbfHeader header = reader.getHeader();

            String[] titles = new String[header.getFieldsCount()];
            for (int i = 0; i < header.getFieldsCount(); i++) {
                DbfField field = header.getField(i);
                titles[i] = StringUtils.rightPad(field.getName(), field.getFieldLength(), ' ');
            }
            for (String title : titles) {
                writer.print(title);
                writer.print(delimit);
            };
            writer.println();
            Object[] row;
            while ((row = reader.nextRecord()) != null) {
                for (int i = 0; i < header.getFieldsCount(); i++) {
                    DbfField field = header.getField(i);
                    if(field.getDataType()== 'C'){
                        if(row[i] != null){
                            writer.print(new String((byte[]) row[i], dbfEncoding));
                        }
                       
                    }else if(field.getDataType() == 'N'){

                        if(row[i] != null){
                            if(field.getDecimalCount()>0){
                                writer.print(((Number) row[i]).doubleValue());
                            }else{
                                writer.print(((Number) row[i]).longValue());
                            }
                        }else{
                            if(field.getDecimalCount()>0){
                                writer.print(0.00);
                            }else{
                                writer.print(0);
                            }
                        }

                    }else{
                        writer.print(String.valueOf(row[i]));
                    }
                    writer.print(delimit);
                }
                /****
                 *  海关月度数据
                 */
                if(dbf.getName().toLowerCase().startsWith("h")){
                    writer.print("20" + dbf.getName().substring(1,3));
                    writer.print(delimit);
                    writer.print(dbf.getName().substring(3,5));
                }
                /***
                 *  季度数据
                 */
                if(dbf.getName().toLowerCase().startsWith("gb")){
                    writer.print("20" + dbf.getName().substring(2,4));
                    writer.print(delimit);
                    writer.print(dbf.getName().substring(4,6));
                }
                if(dbf.getName().contains("产量")){
                    writer.print(dbf.getName().substring(0,4));
                    writer.print(delimit);
                    writer.print(dbf.getName().substring(4,6));
                }
                writer.println();
            }
            writer.close();
            reader.close();
        } catch (Exception e) {
            throw new DbfException("Cannot write .dbf file to .txt", e);
        }
    }
}
