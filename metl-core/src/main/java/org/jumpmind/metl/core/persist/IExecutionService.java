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
package org.jumpmind.metl.core.persist;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;

public interface IExecutionService {

    public void save(AbstractObject object);

    public List<Execution> findExecutions(Map<String, Object> params, int limit);

    public Execution findExecution(String id);

    public List<ExecutionStep> findExecutionSteps(String executionId);
    
    public List<ExecutionStepLog> findExecutionStepLogsInError(String executionStepId);

    public List<ExecutionStepLog> findExecutionStepLogs(String executionStepId, int limit);
    
    public List<ExecutionStepLog> findExecutionStepLogs(Set<String> executionStepIds, int limit);
    
    public void markAbandoned(String agentId);
    
    public void deleteExecution(String executionId);
    
    public List<String> findExecutedFlowIds();

    public File getExecutionStepLog(String executionStepId);
    	
}
