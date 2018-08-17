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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jumpmind.db.sql.SqlScriptReader;
import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlExecutor extends AbstractRdbmsComponentRuntime {

    private static final String FILE = "sql.file";

    private static final String SQL_FROM_MESSAGE = "sql.get.from.message";

    public static final String TYPE = "Sql Executor";    

    List<String> sqls;

    String runWhen = PER_MESSAGE;

    String file;

    boolean getSqlFromMessage = false;
    
    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        getSqlFromMessage = properties.is(SQL_FROM_MESSAGE, getSqlFromMessage);
        file = properties.get(FILE);
        sqls = getExecutorSqlStatements();
        runWhen = properties.get(RUN_WHEN, PER_MESSAGE);
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("This component requires a data source");
        }
    }

    protected List<String> getExecutorSqlStatements() {

        List<String> sqlStatements=null;;

        //sqlstatements come from message, file, or sql setting in the component
        if (!getSqlFromMessage) {
	        if (isNotBlank(file)) {
	            sqlStatements = new ArrayList<String>();
	            FileReader fileReader = null;
	            SqlScriptReader sqlReader = null;
	            try {
	                fileReader = new FileReader(file);
	                sqlReader = new SqlScriptReader(fileReader);
	                String sqlToExecute = sqlReader.readSqlStatement();
	                while (sqlToExecute != null) {
	                    sqlStatements.add(sqlToExecute);
	                    sqlToExecute = sqlReader.readSqlStatement();
	                }
	            } catch (FileNotFoundException e) {
	                throw new MisconfiguredException("Could not find configured file: %s", file);
	            } finally {
	                IOUtils.closeQuietly(fileReader);
	                IOUtils.closeQuietly(sqlReader);
	            }
	        } else {
	            sqlStatements = getSqlStatements(isBlank(file));            
	        }
        }
        return sqlStatements;
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(final Message inputMessage, final ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        results.clear();
        NamedParameterJdbcTemplate template = getJdbcTemplate();
        int sqlCount = 0;
        int resultCount = 0;
        int inboundRecordCount = 0;
        
        Iterator<?> inboundPayload = null;
        
        if (getSqlFromMessage) {
        		if (inputMessage instanceof TextMessage) {
                this.sqls = ((TextMessage)inputMessage).getPayload();
            } 
        }
        
        if (PER_ENTITY.equals(runWhen) && inputMessage instanceof ContentMessage<?>) {
            inboundPayload = ((Collection<?>) ((ContentMessage<?>) inputMessage).getPayload()).iterator();
            inboundRecordCount = ((Collection<?>) ((ContentMessage<?>) inputMessage).getPayload()).size();
        } else if (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage)) {
            inboundPayload = null;
            inboundRecordCount = 1;
        } else if (PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage) {
            inboundPayload = null;
            inboundRecordCount = 1;
        }        
        
        for (int i = 0; i < inboundRecordCount; i++) {
            Object entity = inboundPayload != null && inboundPayload.hasNext() ? inboundPayload.next() : null;
            for (String sql : this.sqls) {
                String sqlToExecute = prepareSql(sql, inputMessage, entity);
                Map<String, Object> paramMap = prepareParams(sqlToExecute, inputMessage, entity, runWhen);
                log(LogLevel.INFO, "About to run: %s", sqlToExecute);
                log(LogLevel.INFO, "Passing params: %s", paramMap);
                resultCount = template.update(sqlToExecute, paramMap);  
                getComponentStatistics().incrementNumberEntitiesProcessed(resultCount);
                sqlCount++;
            }            
        }        
        if (callback != null && sqlCount > 0) {
            callback.sendTextMessage(null, convertResultsToTextPayload(results));
        }
        log(LogLevel.INFO, "Ran %d sql statements", sqlCount);        
    } 
}
