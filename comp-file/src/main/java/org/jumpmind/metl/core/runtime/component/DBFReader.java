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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jamel.dbf.DbfReader;
import org.jamel.dbf.structure.DbfField;
import org.jamel.dbf.structure.DbfHeader;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.alibaba.fastjson.JSON;


public class DBFReader extends AbstractRdbmsComponentRuntime {

    public static final String TYPE = "DBF Reader";

    public static final String SETTING_ENCODING = "encoding";
    
    public static final String DBF_CONF_DIR_PATH = "dbf.conf.dir.path";
    
    public static final String DBF_CONF_MAPPING = "dbf.conf.mapping";
    
    public static final String DBF_CONF_TABLE_MAPPING = "dbf.conf.table.mapping";


    String encoding = "UTF-8";
    
    String dbfConfDirPath = "";
    
    String dbfConfMapping = "";
    
    String dbfConfTableMapping = "";
     
    String runWhen = PER_MESSAGE;

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        encoding = properties.get(SETTING_ENCODING, encoding);
        dbfConfDirPath = properties.get(DBF_CONF_DIR_PATH, "").replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "").replaceAll(" ", "");
        dbfConfMapping = properties.get(DBF_CONF_MAPPING, "").replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "").replaceAll(" ", "");
        dbfConfTableMapping = properties.get(DBF_CONF_TABLE_MAPPING, "").replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "").replaceAll(" ", "");
        if ("".equals(encoding)) {
        	encoding = "UTF-8";
        	log(LogLevel.INFO, "File Encoding has not been set, using the default of UTF-8.");
        }
        runWhen = properties.get(RUN_WHEN, PER_MESSAGE);
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("This component requires a data source");
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
            process(inputMessage, callback, unitOfWorkBoundaryReached);
    }
    /***
     * handle dbf files 
     * @param inputMessage
     * @param callback
     * @param unitOfWorkBoundaryReached
     */
	private void process(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
		//文件或目录
		List<String> paths = Arrays.asList(dbfConfDirPath.trim().split(";"));
		//文件名正则和表对应关系
		Map<String,String> mapping = new HashMap<String, String>();
		Arrays.asList(dbfConfMapping.trim().split(";")).forEach(item->{
			if(StringUtils.isNotBlank(item) && item.contains(":")) {
				String[] subItems = item.split(":");
				mapping.put(subItems[0], subItems[1]);
			}
		});
		//表字段对应关系
		Map<String, TableConfig> tableMaps = 
				JSON.parseArray(dbfConfTableMapping, TableConfig.class)
				.stream().collect(Collectors.toMap(TableConfig::getName, t->t));
		//判断路径
		if(paths != null && paths.size() > 0) {
			//循环处理dbf文件
			paths.forEach(path->{
				//有个需求，当配置的文件或目录  没有dbf文件时   报错
				//处理文件计数器
				int fileCount = 0;
				File file = null;
				try {
					file = new File(path);
					//目录
					if(file.isDirectory()) {
						//子文件
						File[] children = file.listFiles();
						//遍历子文件
						for(File child:children) {
							if(child.isFile() && child.getName().toLowerCase().endsWith(".dbf")) {
								handleFile(child, mapping, tableMaps);
								fileCount++;
							}
							
						}
					}
					//文件
					else{
						if(file.isFile() && file.getName().toLowerCase().endsWith(".dbf")) {
							handleFile(file, mapping, tableMaps);
							fileCount++;
						}
					}
				}catch (Exception e) {
					log(LogLevel.ERROR,"处理文件失败");
					e.printStackTrace();
					throw new RuntimeException("处理文件失败");
				}
				if(fileCount > 0) {
					log(LogLevel.INFO,path + " dbf数据读取成功");
				}else {
					log(LogLevel.WARN,path + " 不是或没有dbf文件");
				}
			});
		}
		
	}
    private void handleFile(File file, Map<String,String> mapping, Map<String, TableConfig> tableMaps) throws Exception {
    	//校验文件类型
		if(file.isFile() && file.getName().toLowerCase().endsWith(".dbf")) {
			
			results.clear();
	        NamedParameterJdbcTemplate template = getJdbcTemplate();
	        
			//确定文件对应表
			String table = "";
			for(String key:mapping.keySet()) {
				if(file.getName().matches(mapping.get(key))) {
					table = key;
					break;
				}
			}
			//无对应表,直接返回
			if(StringUtils.isBlank(table)) return;
			
			//处理数据
			TableConfig tableConfig = tableMaps.get(table);
			if(tableConfig == null) return;
			DbfReader reader = null;
			//读取数据
			try {
				//dbf reader
				reader = new DbfReader(file);
				//dbf header
				DbfHeader header = reader.getHeader();
				//列对应关系反转,以dbf的列名为key
				Map<String, String> keyMap = new HashMap();
				tableConfig.getMapping().forEach((k, v)->{
					keyMap.put(v, k);
				});
				Map<String, Object>[] mapArray =new Map[]{};
				//参数列表
				List<Map<String, Object>> batchParams = new ArrayList<Map<String, Object>>();
				Object[] row;
	            while ((row = reader.nextRecord()) != null) {
	            	//单条参数
	            	Map<String, Object> params = new HashMap<String, Object>();
	                for (int i = 0; i < header.getFieldsCount(); i++) {
	                    DbfField field = header.getField(i);
	                    String column = keyMap.get(String.valueOf(i+1));
	                    if(field.getDataType().byteValue == 'C'){
	                        if(row[i] != null){
	                            params.put(column, new String((byte[]) row[i], this.encoding));
	                        }else {
	                        	params.put(column, "");
	                        }

	                    }else if(field.getDataType().byteValue == 'N'){

	                        if(row[i] != null){
	                            if(field.getDecimalCount()>0){
	                            	params.put(column, ((Number) row[i]).doubleValue());
	                            }else{
	                            	params.put(column, ((Number) row[i]).longValue());
	                            }
	                        }else{
	                            if(field.getDecimalCount()>0){
	                            	params.put(column, 0.00);
	                            }else{
	                            	params.put(column, 0);
	                            }
	                        }

	                    }else{
	                    	params.put(column, String.valueOf(row[i]));
	                    }
	                }
	                batchParams.add(params);
	                if(batchParams.size() > 999) {
	                	System.err.println(tableConfig.getDeleteSQL());
	                	System.err.println(tableConfig.getInsertSQL());
	                	if(tableConfig.key != null && tableConfig.key.size() > 0)
	                	template.batchUpdate(tableConfig.getDeleteSQL(), batchParams.toArray(mapArray));
	                	template.batchUpdate(tableConfig.getInsertSQL(), batchParams.toArray(mapArray));
	                	batchParams = null;
	                	batchParams = new ArrayList<Map<String, Object>>();
	                }
	            }
	            if(batchParams.size()>0) {
	            	if(tableConfig.key != null && tableConfig.key.size() > 0)
	            	template.batchUpdate(tableConfig.getDeleteSQL(), batchParams.toArray(mapArray));
                	template.batchUpdate(tableConfig.getInsertSQL(), batchParams.toArray(mapArray));
	            }
				
				
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}finally {
				if(reader != null) {
					reader.close();
				}
			}
			
			
			
		}
    }

	@Override
	public boolean supportsStartupMessages() {
		// TODO Auto-generated method stub
		return true;
	}
}
