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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SqlExecutor extends AbstractRdbmsComponentRuntime {

    private static final String ON_SUCCESS = "ON SUCCESS";

    private static final String PER_MESSAGE = "PER MESSAGE";

    private static final String PER_ENTITY = "PER ENTITY";

    public static final String TYPE = "Sql Executor";

    public final static String RUN_WHEN = "run.when";

    List<String> sqls;

    String runWhen = PER_MESSAGE;

    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        sqls = getSqlStatements();
        runWhen = properties.get(RUN_WHEN, PER_MESSAGE);
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("This component requires a data source");
        }
    }
        
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(final Message inputMessage, final ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	List<Result> results = new ArrayList<Result>();
        for (String sql : this.sqls) {
            final String sqlToExecute = FormatUtils.replaceTokens(sql,
                    context.getFlowParametersAsString(), true);
            NamedParameterJdbcTemplate template = getJdbcTemplate();
            Map<String, Object> params = getParameters(inputMessage);
            if (runWhen.equals(PER_MESSAGE)) {
                int count = template.update(sqlToExecute, params);
                results.add(new Result(sqlToExecute, count));
                getComponentStatistics().incrementNumberEntitiesProcessed(count);
            } else if (runWhen.equals(PER_ENTITY)) {
                List<EntityData> datas = inputMessage.getPayload();
                for (EntityData entityData : datas) {
                    params.putAll(getComponent().toRow(entityData, false));
                    int count = template.update(sqlToExecute, params);
                    results.add(new Result(sqlToExecute, count));
                    getComponentStatistics().incrementNumberEntitiesProcessed(count);
                }
            }
        }

        if (callback != null) {
        	callback.sendMessage(null, convertResultsToTextPayload(results), unitOfWorkBoundaryReached);
        }
    }

    private HashMap<String, Object> getParameters(Message inputMessage) {
        HashMap<String, Object> params = new HashMap<String, Object>(
                context.getFlowParametersAsString());
        params.putAll(inputMessage.getHeader());
        return params;
    }
    
    @Override
    public void flowCompleted(boolean cancelled) {
        if (runWhen.equals(ON_SUCCESS) && !cancelled) {
            NamedParameterJdbcTemplate template = getJdbcTemplate();
            for (String sql : this.sqls) {
                String sqlToExecute = sql;
                Map<String, Object> params = new HashMap<String, Object>();
                sqlToExecute = FormatUtils.replaceTokens(sql, context.getFlowParametersAsString(),
                        true);
                params.putAll(context.getFlowParameters());
                log(LogLevel.INFO, "Executing the following sql after a successful completion: "
                        + sqlToExecute);
                int count = template.update(sqlToExecute, params);
                getComponentStatistics().incrementNumberEntitiesProcessed(count);
            }
        }
    }

}
