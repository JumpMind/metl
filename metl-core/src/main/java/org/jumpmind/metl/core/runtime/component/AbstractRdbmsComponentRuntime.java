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
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jumpmind.db.sql.SqlScriptReader;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.properties.TypedProperties;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

abstract public class AbstractRdbmsComponentRuntime extends AbstractComponentRuntime {

    public final static String SQL = "sql";

    protected List<Result> results = new ArrayList<Result>();

    protected DataSource dataSource;

    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        if (dataSource == null && getResourceRuntime() == null) {
            throw new RuntimeException("The data source resource has not been configured.  Please configure it.");
        }

        if (dataSource == null) {
            dataSource = (DataSource) this.context.getResourceRuntime().reference();
        }
        return new NamedParameterJdbcTemplate(dataSource);
    }

    protected List<String> getSqlStatements(boolean required) {
        TypedProperties properties = getTypedProperties();
        String script = properties.get(SQL);
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
