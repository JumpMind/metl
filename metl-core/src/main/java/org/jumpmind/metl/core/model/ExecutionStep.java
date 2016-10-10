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
package org.jumpmind.metl.core.model;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ExecutionStep extends AbstractObject {

    private static final long serialVersionUID = 1L;

    private String executionId;

    private int threadNumber = 0;

    private String flowStepId;

    private String componentName;

    private String status;

    private long messagesReceived;

    private long messagesProduced;

    private long payloadReceived;

    private long payloadProduced;

    private long entitiesProcessed;

    private int approximateOrder;

    private Date startTime;

    private Date endTime;

    private long handleDuration = 0;
    
    private long queueDuration = 0;

    public long getHandleDuration() {
        return handleDuration;
    }

    public void setHandleDuration(long handleDuration) {
        this.handleDuration = handleDuration;
    }

    public void incrementHandleDuration(long handleDuration) {
        this.handleDuration += handleDuration;
    }

    public String getHandleDurationString() {
        LocalTime t = LocalTime.MIDNIGHT.plus(Duration.ofMillis(handleDuration));
        return DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(t);
    }
    
    public String getQueueDurationString() {
        LocalTime t = LocalTime.MIDNIGHT.plus(Duration.ofMillis(queueDuration));
        return DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(t);
    }

    public void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getFlowStepId() {
        return flowStepId;
    }

    public void setFlowStepId(String flowStepId) {
        this.flowStepId = flowStepId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public ExecutionStatus getExecutionStatus() {
        return status == null ? null : ExecutionStatus.valueOf(status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getMessagesReceived() {
        return messagesReceived;
    }

    public void setMessagesReceived(long messagesReceived) {
        this.messagesReceived = messagesReceived;
    }

    public long getMessagesProduced() {
        return messagesProduced;
    }

    public void setMessagesProduced(long messagesProduced) {
        this.messagesProduced = messagesProduced;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setEntitiesProcessed(long entitiesProcessed) {
        this.entitiesProcessed = entitiesProcessed;
    }

    public long getEntitiesProcessed() {
        return entitiesProcessed;
    }

    public void setApproximateOrder(int approximateOrder) {
        this.approximateOrder = approximateOrder;
    }

    public int getApproximateOrder() {
        return approximateOrder;
    }

    public void setPayloadProduced(long payloadProduced) {
        this.payloadProduced = payloadProduced;
    }

    public long getPayloadProduced() {
        return payloadProduced;
    }

    public void setPayloadReceived(long payloadReceived) {
        this.payloadReceived = payloadReceived;
    }

    public long getPayloadReceived() {
        return payloadReceived;
    }
    
    public void setQueueDuration(long queueDuration) {
        this.queueDuration = queueDuration;
    }
    
    public long getQueueDuration() {
        return queueDuration;
    }

}
