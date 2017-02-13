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
package org.jumpmind.metl.core.runtime;

import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.jumpmind.metl.core.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionTrackerLogger implements IExecutionTracker {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    AgentProjectVersionFlowDeployment deployment;
    
    String executionId;
    
    Map<String, Long> stepStartTimes = new HashMap<String, Long>();

    public ExecutionTrackerLogger(AgentProjectVersionFlowDeployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public void beforeFlow(String executionId, Map<String, String> flowParameters) {
        this.executionId = executionId;
        String msg = String.format("[%s] Flow started for deployment: %s with parameters: %s", executionId, deployment.getName(), flowParameters);
        log.info(msg);
    }

    @Override
    public void afterFlow() {
        String msg = String.format("[%s] Flow finished for deployment: %s", executionId, deployment.getName());
        log.info(msg);
    }

    @Override
    public void beforeHandle(int threadNumber, ComponentContext context) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "[%s] Handling message for deployment: %s for component: %s", executionId, 
                deployment.getName(), flowStep.getName());
        log.debug(msg);
    }

    @Override
    public void afterHandle(int threadNumber, ComponentContext context, Throwable error) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "[%s] Finished handling message for deployment: %s for component: %s", executionId, 
                deployment.getName(), flowStep.getName());
        log.debug(msg);
    }
    
    @Override
    public void updateStatistics(int threadNumber, ComponentContext component) {
    }
    
    @Override
    public void flowStepStarted(int threadNumber, ComponentContext context) {
        FlowStep flowStep = context.getFlowStep();
        stepStartTimes.put(flowStep.getId(), System.currentTimeMillis());
        String msg = String.format(
                "[%s] Started flow step for deployment: %s for component: %s", executionId, 
                deployment.getName(), flowStep.getName());
        log.info(msg);
    }
    
    @Override
    public void flowStepFinished(int threadNumber, ComponentContext context, Throwable error, boolean cancelled) {
        FlowStep flowStep = context.getFlowStep();
        Long startTime = stepStartTimes.get(flowStep.getId());        
        long duration = startTime == null ? 0 : System.currentTimeMillis() - startTime;        
        String msg = String.format(
                "[%s] Finished flow step for deployment: %s for component: %s in %s", executionId, 
                deployment.getName(), flowStep.getName(), LogUtils.formatDuration(duration));
        log.info(msg);
    }
    
    @Override
    public void flowStepFailedOnComplete(ComponentContext context, Throwable error) {
    }

    @Override
    public void log(int threadNumber, LogLevel level, ComponentContext context, String output, Object...args) {
        if (deployment.asLogLevel().log(level)) {
            if (args != null && args.length > 0) {
                output = String.format(output, args);
            }
            switch (level) {
                case DEBUG:
                    log.debug("[{}] {}", executionId, output);
                    break;
                case INFO:
                    log.info("[{}] {}", executionId, output);
                    break;
                case WARN:
                    log.warn("[{}] {}", executionId, output);
                    break;
                case ERROR:
                default:
                    log.error("[{}] {}", executionId, output);
                    break;
            }
        }
    }
    
    @Override
    public String getExecutionId() {
        return executionId;
    }
}
