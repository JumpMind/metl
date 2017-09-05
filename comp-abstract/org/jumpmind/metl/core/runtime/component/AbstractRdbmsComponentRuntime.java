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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.sql.SqlScriptReader;
import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.properties.TypedProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * This is in the process of moving to comp-abstract
 */
abstract public class AbstractRdbmsComponentRuntime extends AbstractComponentRuntime {

    public final static String SQL = "sql";
    
    public final static String DDL = "ddl";
    
    public final static String QUERY_TIMEOUT = "query.timeout.seconds";

    protected List<Result> results = new ArrayList<Result>();

    protected DataSource dataSource;
    
    protected int queryTimeout = -1;

    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        if (dataSource == null && getResourceRuntime() == null) {
            throw new RuntimeException("The data source resource has not been configured.  Please configure it.");
        }

        if (dataSource == null) {
            dataSource = (DataSource) getResourceRuntime().reference();
        }
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setQueryTimeout(queryTimeout);
        return new NamedParameterJdbcTemplate(template);
    }

    protected List<String> getSqlStatements(boolean required) {
        TypedProperties properties = getTypedProperties();
        String script = properties.get(SQL);
        return getStatements(script, required);
    }
    
    protected List<String> getDdlStatements(boolean required) {
    	TypedProperties properties = getTypedProperties();
        String script = properties.get(DDL);
        return getStatements(script, required);
    }
    
    private List<String> getStatements(String script, boolean required) {
        if (isNotBlank(script)) {
            List<String> sqlStatements = new ArrayList<String>();
            SqlScriptReader scriptReader = new SqlScriptReader(new StringReader(script));
            try {
                String sql = scriptReader.readSqlStatement();
                while (sql != null) {
                    sqlStatements.add(sql);
                    sql = scriptReader.readSqlStatement();
                }
                return sqlStatements;
            } finally {
                IOUtils.closeQuietly(scriptReader);
            }
        } else if (required) {
            throw new MisconfiguredException("Please configure the SQL for %s", componentDefinition.getName());
        } else {
            return Collections.emptyList();
        }
    }
    
    protected String prepareSql(String sql, Message inputMessage, Object entity) {
        sql = resolveParamsAndHeaders(sql, inputMessage);
        return sql;
    }    
    
    protected Map<String, Object> prepareParams(String sql, Message inputMessage, Object entity, String runWhen) {
        Map<String, Object> paramMap = new HashMap<>();
        /*
         * input parameters can come from the header and the record. header
         * parms should be used for every record.
         */
        paramMap.putAll(context.getFlowParameters() == null ? Collections.emptyMap() : context.getFlowParameters());
        paramMap.putAll(inputMessage.getHeader());
        if (entity instanceof EntityData) {
            EntityData entityData = (EntityData) entity;
            paramMap.putAll(this.getComponent().toRow(entityData, true, true));
        } else if (entity != null) {
            paramMap.put("RECORD", entity.toString());
        }

        if (PER_MESSAGE.equals(runWhen) && inputMessage instanceof ContentMessage<?>) {
            if (((ContentMessage<?>) inputMessage).getPayload() instanceof Collection) {
                Collection<?> payload = (Collection<?>) ((ContentMessage<?>) inputMessage).getPayload();
                enhanceParamMapWithInValues(paramMap, payload, sql);
            }
        }
        return paramMap;
    }

    protected void enhanceParamMapWithInValues(Map<String, Object> paramMap, Collection<?> payload, String sql) {
        Set<String> attributeNames = findWhereInParameters(sql);
        for (String attributeName : attributeNames) {
            List<Object> in = new ArrayList<>();
            Iterator<?> i = payload.iterator();
            while (i.hasNext()) {
                Object next = i.next();
                if (next instanceof EntityData) {
                    EntityData entityData = (EntityData) next;
                    Row row = this.getComponent().toRow(entityData, true, true);
                    Object value = row.get(attributeName);
                    if (value != null) {
                        in.add(value);
                    }
                }
            }
            paramMap.put(attributeName, in);
        }
    }

    protected Set<String> findWhereInParameters(String sql) {
        Set<String> inAttributeNames = new HashSet<>();
        List<Integer> indexes = new ArrayList<>();
        int currentIndex = -4;
        do {
            int index = sql.indexOf(" in ", currentIndex + 4);
            if (index < 0) {
                index = sql.indexOf(" IN ", currentIndex + 4);
            }
            currentIndex = index;
            if (currentIndex > 0) {
                indexes.add(currentIndex);
                int left = sql.indexOf("(", currentIndex + 4);
                int right = sql.indexOf(")", currentIndex + 4);
                if (left > 0 && right > 0) {
                    String attributeId = sql.substring(left + 1, right).trim();
                    attributeId = attributeId.substring(1);
                    inAttributeNames.add(attributeId);
                }
            }
        } while (currentIndex > 0 && currentIndex < sql.length());

        return inAttributeNames;
    }    
        
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<String> convertResultsToTextPayload(List<Result> results) {
        ArrayList<String> payload = new ArrayList<String>();
        JSONArray jsonResults = new JSONArray();
        for (Result result : results) {
            JSONObject jsonResult = new JSONObject();
            jsonResult.put("Sql", result.sql);
            jsonResult.put("Rows Affected", result.numberRowsAffected);
            jsonResults.add(jsonResult);
        }
        payload.add(jsonResults.toJSONString());
        return payload;
    }

    class Result {
        String sql;
        int numberRowsAffected;

        Result(String sql, int numberRowsAffected) {
            this.sql = sql;
            this.numberRowsAffected = numberRowsAffected;
        }
    }

}
