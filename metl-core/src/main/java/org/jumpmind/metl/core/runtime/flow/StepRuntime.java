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
package org.jumpmind.metl.core.runtime.flow;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.IExecutionTracker;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.jumpmind.metl.core.runtime.component.ComponentStatistics;
import org.jumpmind.metl.core.runtime.component.IComponentRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepRuntime implements Runnable {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String UNIT_OF_WORK = "unit.of.work";
    
    public static final String UNIT_OF_WORK_INPUT_MESSAGE = "Input Message";
    
    public static final String UNIT_OF_WORK_FLOW = "Flow";

    protected BlockingQueue<Message> inQueue;

    boolean running = false;

    boolean cancelled = false;
    
    Throwable error;

    IComponentRuntime componentRuntime;

    List<StepRuntime> targetStepRuntimes;

    List<StepRuntime> sourceStepRuntimes;
    
    Map<String, Boolean> sourceStepRuntimeUnitOfWorkReceived;

    ComponentContext componentContext;

    FlowRuntime flowRuntime;

    public StepRuntime(IComponentRuntime componentRuntime, ComponentContext componentContext, FlowRuntime flowRuntime) {
        this.flowRuntime = flowRuntime;
        this.componentContext = componentContext;
        this.componentRuntime = componentRuntime;
        int capacity = componentContext.getFlowStep().getComponent().getInt(AbstractComponentRuntime.INBOUND_QUEUE_CAPACITY, 1000);
        inQueue = new LinkedBlockingQueue<Message>(capacity);
        sourceStepRuntimeUnitOfWorkReceived = new HashMap<String, Boolean>();
    }
    
    public boolean isStartStep() {
        return sourceStepRuntimes == null || sourceStepRuntimes.size() == 0;
    }

    public void setTargetStepRuntimes(List<StepRuntime> targetStepRuntimes) {
        this.targetStepRuntimes = targetStepRuntimes;
    }

    public void setSourceStepRuntimes(List<StepRuntime> sourceStepRuntimes) {
        this.sourceStepRuntimes = sourceStepRuntimes;
    }

    protected void queue(Message message) throws InterruptedException {
        if (inQueue.remainingCapacity() == 0
                && message.getHeader().getOriginatingStepId().equalsIgnoreCase(componentRuntime.getComponentContext().getFlowStep().getId())) {
            throw new RuntimeException("Inbound queue capacity on " + componentRuntime.getComponentContext().getFlowStep().getName()
                    + " not sufficient to handle inbound messages from other components in addition to inbound messages from itself.");
        }
        inQueue.put(message);
    }

    public void start(IExecutionTracker tracker, IResourceFactory resourceFactory) {
        try {
            componentContext.getExecutionTracker().flowStepStarted(componentContext);
            componentContext.setComponentStatistics(new ComponentStatistics());
            componentRuntime.start(componentContext);
        } catch (RuntimeException ex) {
            recordError(ex);
            throw ex;
        }
    }

    protected void recordError(Throwable ex) {
        error = ex;
        String msg = ex.getMessage();
        if (isBlank(msg)) {
            msg = ExceptionUtils.getFullStackTrace(ex);
        }
        componentContext.getExecutionTracker().log(LogLevel.ERROR, componentContext, msg);
        log.error("", ex);
        flowRuntime.stop(false);
    }

    @Override
    public void run() {
        try {
            SendMessageCallback target = new SendMessageCallback();
            /*
             * if we are a start step (don't have any input links), we'll only
             * get a single message which is the start message sent by the flow
             * runtime to kick things off. If we have input links, we must loop
             * until we get a shutdown message from one of our sources
             */
            while (flowRuntime.isRunning()) {
                /*
                 * continue to poll as long as the flow is running. other
                 * components could be generating messages which could block if
                 * we don't continue to poll
                 */
                Message inputMessage = inQueue.poll(5, TimeUnit.SECONDS);
                if (running) {
                    if (inputMessage instanceof ShutdownMessage) {
                        ShutdownMessage shutdownMessage = (ShutdownMessage) inputMessage;

                        cancelled = shutdownMessage.isCancelled();

                        String fromStepId = inputMessage.getHeader().getOriginatingStepId();
                        removeSourceStepRuntime(fromStepId);
                        /*
                         * When all of the source step runtimes have been
                         * removed or when the shutdown message comes from
                         * myself, then go ahead and shutdown
                         */
                        if (cancelled || fromStepId == null || sourceStepRuntimes == null || sourceStepRuntimes.size() == 0
                                || fromStepId.equals(componentContext.getFlowStep().getId())) {
                            shutdown(target);
                        }
                    } else if (inputMessage != null) {
                        try {
                            componentContext.getExecutionTracker().beforeHandle(componentContext);
                            componentContext.getComponentStatistics().incrementInboundMessages();
                            componentRuntime.handle(inputMessage, target, calculateUnitOfWorkLastMessage(inputMessage));
                        } catch (Exception ex) {
                            recordError(ex);
                        } finally {
                            componentContext.getExecutionTracker().afterHandle(componentContext, error);
                        }
                        if (isStartStep() ||
                        // TODO: this only works if the loop is to yourself.
                        // Larger loop detection and processing needed
                                (sourceStepRuntimes.size() == 1
                                        && sourceStepRuntimes.get(0).getComponentContext().equals(this.componentContext) && inQueue.size() == 0)) {
                            shutdown(target);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TODO: notify the flow runtime that we have an error and let it
            // gracefully shut things down
            log.error("", ex);
            error = ex;
        }
    }

    private boolean calculateUnitOfWorkLastMessage(Message inputMessage) {
        boolean lastMessage = true;
        if (inputMessage.getHeader().isUnitOfWorkLastMessage()) {
            sourceStepRuntimeUnitOfWorkReceived.put(inputMessage.getHeader().getOriginatingStepId(), Boolean.TRUE);
        }
        for (StepRuntime sourceRuntime : sourceStepRuntimes) {
            lastMessage &= sourceStepRuntimeUnitOfWorkReceived.get(sourceRuntime.getComponentContext().getFlowStep().getId()) != null;
        }
        if (lastMessage) {
            //sourceStepRuntimeUnitOfWorkReceived.clear();
        }
        return lastMessage;
    }
    
    private void removeSourceStepRuntime(String stepId) {
        if (sourceStepRuntimes != null) {
            Iterator<StepRuntime> it = sourceStepRuntimes.iterator();
            while (it.hasNext()) {
                StepRuntime sourceRuntime = (StepRuntime) it.next();
                if (sourceRuntime.getComponentContext().getFlowStep().getId().equals(stepId)) {
                    it.remove();
                }
            }
        }
    }

    private void shutdown(SendMessageCallback target) throws InterruptedException {
        for (StepRuntime targetStepRuntime : targetStepRuntimes) {
            targetStepRuntime.queue(new ShutdownMessage(componentContext.getFlowStep().getId(), cancelled));
        }
        try {
            this.componentRuntime.stop();
        } catch (Exception e) {
            recordError(e);
        }
        running = false;
        finished();
    }

    public void finished() {
        componentContext.getExecutionTracker().flowStepFinished(componentContext, error, cancelled);
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    public void flowCompletedWithoutError() {
        if (!cancelled) {
            try {
                componentRuntime.flowCompleted(cancelled);
            } catch (Exception ex) {
                recordError(ex);
                componentContext.getExecutionTracker().flowStepFailedOnComplete(componentContext, ex);
            }
        }
    }

    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors) {
        if (!cancelled) {
            try {
                componentRuntime.flowCompletedWithErrors(myError);
            } catch (Exception ex) {
                recordError(ex);
                componentContext.getExecutionTracker().flowStepFailedOnComplete(componentContext, ex);
            }
        }
    }

    public IComponentRuntime getComponentRuntime() {
        return this.componentRuntime;
    }

    public Throwable getError() {
        return error;
    }

    public ComponentContext getComponentContext() {
        return componentContext;
    }

    class SendMessageCallback implements ISendMessageCallback {
        
        protected boolean isUnitOfWorkLastMessage(String unitOfWork, boolean lastMessage) {
            if (unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_INPUT_MESSAGE) || (unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_FLOW) && lastMessage)) {
                return true;
            } else {
                return false;
            }
        }
        
        protected Message createMessage(Message newMessage, Serializable payload, boolean lastMessage) {
            FlowStep flowStep = componentContext.getFlowStep();
            ComponentStatistics statistics = componentContext.getComponentStatistics();
            String unitOfWork = flowStep.getComponent().get(UNIT_OF_WORK, UNIT_OF_WORK_FLOW);
            newMessage.getHeader().setUnitOfWorkLastMessage(isUnitOfWorkLastMessage(unitOfWork, lastMessage));
            newMessage.getHeader().setSequenceNumber(statistics.getNumberOutboundMessages()+1);
            newMessage.setPayload(payload);
            return newMessage;
        }
        
        protected Serializable copy(Serializable payload) {
            if (payload instanceof ArrayList) {
                payload = (Serializable)((ArrayList<?>)payload).clone();
                ArrayList<?> old = (ArrayList<?>)payload;
                ArrayList<Object> copied = new ArrayList<>(old.size());
                for (Object object : old) {
                    if (object instanceof EntityData) {
                        object = ((EntityData)object).copy();
                    }
                    copied.add(object);
                }
                payload = copied;
            }
            return payload;
        }
        
        protected void sendMessage(Message message, String... targetFlowStepIds) {
            ComponentStatistics statistics = componentContext.getComponentStatistics();
            statistics.incrementOutboundMessages();

            Collection<String> targetStepIds = targetFlowStepIds != null ? Arrays.asList(targetFlowStepIds) : Collections.emptyList();
            
            for (StepRuntime targetRuntime : targetStepRuntimes) {
                boolean forward = targetStepIds == null || targetStepIds.size() == 0
                        || targetStepIds.contains(targetRuntime.getComponentRuntime().getComponentContext().getFlowStep().getId());
                if (forward) {
                    try {
                        targetRuntime.queue(message);
                    } catch (Exception e) {
                        if (e instanceof RuntimeException) {
                            throw (RuntimeException) e;
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        
        @Override
        public void sendShutdownMessage(boolean cancel) {
            FlowStep flowStep = componentContext.getFlowStep();
            sendMessage(new ShutdownMessage(flowStep.getId(), cancel));
        }
        
        @Override
        public void sendStartupMessage() {
            FlowStep flowStep = componentContext.getFlowStep();
            sendMessage(new StartupMessage(flowStep.getId()));
        }        
        
        @Override
        public void sendMessage(Serializable payload, boolean lastMessage, String... targetFlowStepIds) {
            payload = copy(payload);            
            FlowStep flowStep = componentContext.getFlowStep();
            sendMessage(createMessage(new Message(flowStep.getId()), payload, lastMessage), targetFlowStepIds);                 
            
        }
    }

    @Override
    public String toString() {
        return componentContext.getFlowStep().getName();
    }

}
