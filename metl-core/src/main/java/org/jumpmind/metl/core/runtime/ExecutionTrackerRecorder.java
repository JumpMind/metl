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

import static org.apache.commons.lang.StringUtils.abbreviate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.jumpmind.metl.core.runtime.component.ComponentStatistics;
import org.jumpmind.metl.core.runtime.flow.AsyncRecorder;
import org.jumpmind.util.AppUtils;

public class ExecutionTrackerRecorder extends ExecutionTrackerLogger {

    final long TIME_BETWEEN_MESSAGE_UPDATES_IN_MS = 2500;

    AsyncRecorder recorder;

    Agent agent;

    Map<String, ExecutionStep> steps;

    Map<ExecutionStep, Date> lastStatUpdate = new HashMap<ExecutionStep, Date>();

    Date startTime;

    String userId;

    String parameters;

    public ExecutionTrackerRecorder(Agent agent, AgentProjectVersionFlowDeployment agentDeployment, ExecutorService threadService,
            IExecutionService executionService, String userId, String parameters) {
        super(agentDeployment);
        this.agent = agent;
        this.userId = userId;
        this.parameters = parameters;
        this.recorder = new AsyncRecorder(executionService);
        threadService.execute(this.recorder);
    }

    @Override
    public void beforeFlow(String executionId, Map<String, String> flowParameters) {
        super.beforeFlow(executionId, flowParameters);
        this.steps = new HashMap<String, ExecutionStep>();
        this.startTime = new Date();
        this.recorder.record(getExecution());
    }

    private Execution getExecution() {
        Execution execution = new Execution();
        execution.setId(executionId);
        execution.setStartTime(startTime);
        execution.setStatus(ExecutionStatus.RUNNING.name());
        execution.setAgentId(deployment.getAgentDeployment().getAgentId());
        execution.setFlowId(deployment.getAgentDeployment().getFlowId());
        execution.setAgentName(agent.getName());
        execution.setHostName(AppUtils.getHostName());
        execution.setFlowName(deployment.getFlow().getName());
        execution.setDeploymentName(deployment.getName());
        execution.setDeploymentId(deployment.getAgentDeployment().getId());
        execution.setLastUpdateTime(new Date());
        execution.setCreateBy(userId);
        execution.setLastUpdateBy(userId);
        execution.setParameters(abbreviate(parameters, 4000));
        return execution;
    }

    @Override
    public void afterFlow() {
        super.afterFlow();
        Execution execution = getExecution();
        execution.setEndTime(new Date());
        ExecutionStatus status = ExecutionStatus.DONE;
        if (steps != null) {
            for (ExecutionStep executionStep : steps.values()) {
                if (ExecutionStatus.ERROR.name().equals(executionStep.getStatus())) {
                    status = ExecutionStatus.ERROR;
                }

                if (status != ExecutionStatus.ERROR && ExecutionStatus.CANCELLED.name().equals(executionStep.getStatus())) {
                    status = ExecutionStatus.CANCELLED;
                }
            }
        }
        execution.setStatus(status.name());
        this.recorder.record(execution);
        this.recorder.shutdown();
    }

    @Override
    public void flowStepStarted(int threadNumber, ComponentContext context) {
        super.flowStepStarted(threadNumber, context);
        ExecutionStep step = getExecutionStep(threadNumber, context);
        if (!deployment.getAgentDeployment().getLogLevel().equals(LogLevel.OFF.toString())) {
            this.recorder.record(step);
        }
    }

    protected ExecutionStep getExecutionStep(int threadNumber, ComponentContext context) {
        String id = context.getFlowStep().getId() + "-" + threadNumber;
        ExecutionStep step = steps.get(id);
        if (step == null) {
            step = new ExecutionStep();
            step.setStatus(ExecutionStatus.READY.name());
            step.setExecutionId(executionId);
            step.setThreadNumber(threadNumber);
            step.setApproximateOrder(context.getFlowStep().getApproximateOrder());
            step.setComponentName(context.getFlowStep().getComponent().getName());
            step.setFlowStepId(context.getFlowStep().getId());
            this.steps.put(id, step);
        }
        return step;
    }

    @Override
    public void beforeHandle(int threadNumber, ComponentContext context) {
        super.beforeHandle(threadNumber, context);

        ExecutionStep step = getExecutionStep(threadNumber, context);
        Date lastUpdateTime = step.getLastUpdateTime();
        if (step.getStatus().equals(ExecutionStatus.READY.name()) || lastUpdateTime == null
                || (System.currentTimeMillis() - lastUpdateTime.getTime() > TIME_BETWEEN_MESSAGE_UPDATES_IN_MS)) {
            if (step.getStartTime() == null) {
                step.setStartTime(new Date());
            }
            if (!step.getStatus().equals(ExecutionStatus.ERROR.name())) {
                step.setStatus(ExecutionStatus.RUNNING.name());
            }
            step.setLastUpdateTime(new Date());
            if (!deployment.getAgentDeployment().getLogLevel().equals(LogLevel.OFF.toString())) {
                this.recorder.record(step);
            }
        }
    }

    @Override
    public void updateStatistics(int threadNumber, ComponentContext context) {
        super.updateStatistics(threadNumber, context);
        ExecutionStep step = getExecutionStep(threadNumber, context);
        Date lastUpdateTime = lastStatUpdate.get(step);
        if (lastUpdateTime == null || (System.currentTimeMillis() - lastUpdateTime.getTime() > TIME_BETWEEN_MESSAGE_UPDATES_IN_MS)) {
            ComponentStatistics stats = context.getComponentStatistics();
            if (stats != null) {
                step.setEntitiesProcessed(stats.getNumberEntitiesProcessed(threadNumber));
                step.setMessagesReceived(stats.getNumberInboundMessages(threadNumber));
                step.setMessagesProduced(stats.getNumberOutboundMessages(threadNumber));
                step.setPayloadProduced(stats.getNumberOutboundPayload(threadNumber));
                step.setPayloadReceived(stats.getNumberInboundPayload(threadNumber));
                step.setHandleDuration(stats.getTimeSpentInHandle(threadNumber));
                step.setQueueDuration(stats.getTimeSpentWaiting(threadNumber));
                lastStatUpdate.put(step, new Date());
            }
            step.setLastUpdateTime(new Date());
            if (!deployment.getAgentDeployment().getLogLevel().equals(LogLevel.OFF.toString())) {
               this.recorder.record(step);
            }
        }
    }

    @Override
    public void afterHandle(int threadNumber, ComponentContext context, Throwable error) {
        super.afterHandle(threadNumber, context, error);
        ExecutionStep step = getExecutionStep(threadNumber, context);
        Date lastUpdateTime = lastStatUpdate.get(step);

        if (lastUpdateTime == null || (System.currentTimeMillis() - lastUpdateTime.getTime() > TIME_BETWEEN_MESSAGE_UPDATES_IN_MS)) {
            step.setStatus(error != null ? ExecutionStatus.ERROR.name() : ExecutionStatus.READY.name());
            ComponentStatistics stats = context.getComponentStatistics();
            if (stats != null) {
                step.setEntitiesProcessed(stats.getNumberEntitiesProcessed(threadNumber));
                step.setMessagesReceived(stats.getNumberInboundMessages(threadNumber));
                step.setMessagesProduced(stats.getNumberOutboundMessages(threadNumber));
                step.setPayloadProduced(stats.getNumberOutboundPayload(threadNumber));
                step.setPayloadReceived(stats.getNumberInboundPayload(threadNumber));
                step.setHandleDuration(stats.getTimeSpentInHandle(threadNumber));
                step.setQueueDuration(stats.getTimeSpentWaiting(threadNumber));
                lastStatUpdate.put(step, new Date());
            }
            step.setLastUpdateTime(new Date());
            if (!deployment.getAgentDeployment().getLogLevel().equals(LogLevel.OFF.toString())) {
                this.recorder.record(step);
            }
        }
    }

    @Override
    public void flowStepFinished(int threadNumber, ComponentContext context, Throwable error, boolean cancelled) {
        super.flowStepFinished(threadNumber, context, error, cancelled);
        ExecutionStep step = getExecutionStep(threadNumber, context);
        if (step.getStartTime() == null) {
            step.setStartTime(new Date());
        }
        step.setEndTime(new Date());
        ExecutionStatus status;
        if (error != null) {
            status = ExecutionStatus.ERROR;
        } else if (cancelled) {
            status = ExecutionStatus.CANCELLED;
        } else {
            status = ExecutionStatus.DONE;
        }
        step.setStatus(status.name());
        ComponentStatistics stats = context.getComponentStatistics();
        if (stats != null) {
            step.setEntitiesProcessed(stats.getNumberEntitiesProcessed(threadNumber));
            step.setMessagesReceived(stats.getNumberInboundMessages(threadNumber));
            step.setMessagesProduced(stats.getNumberOutboundMessages(threadNumber));
            step.setPayloadProduced(stats.getNumberOutboundPayload(threadNumber));
            step.setPayloadReceived(stats.getNumberInboundPayload(threadNumber));
            step.setHandleDuration(stats.getTimeSpentInHandle(threadNumber));
            step.setQueueDuration(stats.getTimeSpentWaiting(threadNumber));
            lastStatUpdate.put(step, new Date());
        }
        step.setLastUpdateTime(new Date());
        if (!deployment.getAgentDeployment().getLogLevel().equals(LogLevel.OFF.toString()) || 
                ExecutionStatus.ERROR.toString().equals(step.getStatus())) {
           this.recorder.record(step);
        }
    }

    @Override
    public void flowStepFailedOnComplete(ComponentContext context, Throwable error) {
        super.flowStepFailedOnComplete(context, error);
        ExecutionStep step = getExecutionStep(1, context);
        if (step != null) {
            setToErrorStatus(step);
        }
    }

    private void setToErrorStatus(ExecutionStep step) {
        step.setStatus(ExecutionStatus.ERROR.name());
        step.setLastUpdateTime(new Date());
        this.recorder.record(step);
    }

    @Override
    public void log(int threadNumber, LogLevel level, ComponentContext context, String output, Object... args) {
        if (deployment.asLogLevel().log(level)) {
            boolean isError = level.equals(LogLevel.ERROR);
            ExecutionStepLog log = new ExecutionStepLog();
            log.setExecutionStepId(getExecutionStep(threadNumber, context).getId());
            log.setLevel(level.name());
            if (args != null && args.length > 0) {
                output = String.format(output, args);
            }
            log.setLogText(output);
            if (!deployment.getAgentDeployment().getLogLevel().equals(LogLevel.OFF.toString()) || isError) {
               this.recorder.record(log);
            }
        }
    }

}
