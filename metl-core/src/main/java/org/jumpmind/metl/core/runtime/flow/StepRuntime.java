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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MessageHeader;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.core.runtime.component.AssertException;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.jumpmind.metl.core.runtime.component.ComponentStatistics;
import org.jumpmind.metl.core.runtime.component.IComponentRuntime;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.util.ThreadUtils;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepRuntime implements Runnable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String THREAD_COUNT = "thread.count";

    public static final String UNIT_OF_WORK_INPUT_MESSAGE = "Input Message";

    public static final String UNIT_OF_WORK_FLOW = "Flow";

    protected BlockingQueue<Message> inQueue;

    protected Executor componentRuntimeExecutor;

    boolean running = false;

    boolean cancelled = false;

    boolean finished = false;

    Throwable error;

    List<StepRuntime> targetStepRuntimes;

    List<StepRuntime> sourceStepRuntimes;

    Map<String, Boolean> sourceStepRuntimeUnitOfWorkReceived;

    Set<String> targetStepRuntimeUnitOfWorkSent;

    ComponentContext componentContext;

    IComponentRuntimeFactory componentFactory;

    XMLComponent componentDefintion;

    FlowRuntime flowRuntime;

    Map<Integer, IComponentRuntime> componentRuntimeByThread = new HashMap<>();

    Boolean startStep = null;

    Set<String> liveSourceStepIds;

    int contentMessagesSentCount;

    int controlMessagesSentCount;

    int activeCount;
    
    int queueCapacity;
    
    int threadCount;

    public StepRuntime(IComponentRuntimeFactory componentFactory, ComponentContext componentContext, FlowRuntime flowRuntime) {
        this.flowRuntime = flowRuntime;
        this.componentContext = componentContext;
        this.queueCapacity = componentContext.getFlowStep().getComponent().getInt(AbstractComponentRuntime.INBOUND_QUEUE_CAPACITY, 1000);
        this.inQueue = new LinkedBlockingQueue<Message>(queueCapacity);
        this.sourceStepRuntimeUnitOfWorkReceived = new HashMap<String, Boolean>();
        this.targetStepRuntimeUnitOfWorkSent = new HashSet<String>();
        this.componentFactory = componentFactory;
        this.componentDefintion = componentFactory.getComonentDefinition(getComponentType());
    }

    private String getComponentType() {
        return componentContext.getFlowStep().getComponent().getType();
    }

    public int getContentMessagesSentCount() {
        return contentMessagesSentCount;
    }

    public int getControlMessagesSentCount() {
        return controlMessagesSentCount;
    }

    public boolean isQueueEmpty() {
        Message message = this.inQueue.peek();
        return message == null || message instanceof ShutdownMessage;
    }

    public void setTargetStepRuntimes(List<StepRuntime> targetStepRuntimes) {
        this.targetStepRuntimes = targetStepRuntimes;
    }

    public List<StepRuntime> getTargetStepRuntimes() {
        return targetStepRuntimes;
    }

    public void setSourceStepRuntimes(List<StepRuntime> sourceStepRuntimes) {
        this.sourceStepRuntimes = sourceStepRuntimes;
        this.liveSourceStepIds = new HashSet<>();
        for (StepRuntime stepRuntime : sourceStepRuntimes) {
            this.liveSourceStepIds.add(stepRuntime.getComponentContext().getFlowStep().getId());
        }
    }

    public List<StepRuntime> getSourceStepRuntimes() {
        return sourceStepRuntimes;
    }

    protected void queue(Message message) throws InterruptedException {
        if (inQueue.remainingCapacity() == 0
                && message.getHeader().getOriginatingStepId().equalsIgnoreCase(componentContext.getFlowStep().getId())) {
            throw new RuntimeException("Inbound queue capacity on " + componentContext.getFlowStep().getName()
                    + " not sufficient to handle inbound messages from other components in addition to inbound messages from itself.");
        }
        inQueue.put(message);
    }

    public void start(IResourceFactory resourceFactory) {
        try {
            componentContext.setComponentStatistics(new ComponentStatistics());
            startStep = sourceStepRuntimes == null || sourceStepRuntimes.size() == 0;
            Component component = componentContext.getFlowStep().getComponent();
            threadCount = component.getInt(StepRuntime.THREAD_COUNT, 1);            
            if (threadCount > 1) {
                String prefix = String.format("%s-%s", LogUtils.normalizeName(flowRuntime.getAgent().getName()),
                        LogUtils.normalizeName(componentContext.getFlowStep().getName()));
                this.componentRuntimeExecutor = ThreadUtils.createFixedThreadPool(prefix, queueCapacity, threadCount);
            }
            for (int threadNumber = 1; threadNumber <= threadCount; threadNumber++) {
                createComponentRuntime(threadNumber);
            }
        } catch (RuntimeException ex) {
            recordError(1, ex);
            throw ex;
        }
    }

    protected void createComponentRuntime(int threadNumber) {
        String type = getComponentType();
        IComponentRuntime componentRuntime = componentFactory.create(type);
        componentRuntimeByThread.put(threadNumber, componentRuntime);
        if (sourceStepRuntimes.size() == 0 && !componentRuntime.supportsStartupMessages()) {
            throw new MisconfiguredException("%s must have an inbound connection from another component",
                    componentRuntime.getComponentDefintion().getName());
        } else {
            componentContext.getExecutionTracker().flowStepStarted(threadNumber, componentContext);
            componentRuntime.start(threadNumber, componentContext);
        }
    }

    protected void recordError(int threadNumber, Throwable ex) {
        String msg = null;
        if (ex instanceof MisconfiguredException || ex instanceof AssertException) {
            msg = ex.getMessage();
        } else {
            msg = ExceptionUtils.getFullStackTrace(ex);
        }

        componentContext.getExecutionTracker().log(threadNumber, LogLevel.ERROR, componentContext, msg);

        /*
         * First time through here
         */
        if (error == null) {
            error = ex;
            flowRuntime.cancel();
        }
    }

    protected SendMessageCallback createSendMessageCallback() {
        return new SendMessageCallback();
    }

    @Override
    public void run() {
        try {
            SendMessageCallback target = createSendMessageCallback();
            /*
             * If we are a start step (don't have any input links), we'll only
             * get a single message which is the start message sent by the flow
             * runtime to kick things off. If we have input links, we must loop
             * until we get a shutdown message from one of our sources
             */
            while (running && !cancelled) {
                /*
                 * Continue to poll as long as the flow is running. other
                 * components could be generating messages which could block if
                 * we don't continue to poll
                 */
                Message inputMessage = null;
                synchronized (this) {
                    inputMessage = inQueue.poll();
                    if (inputMessage != null && !(inputMessage instanceof ShutdownMessage)) {
                        activeCount++;
                    }
                }
                if (running && !cancelled) {
                    if (inputMessage != null) {
                        if (inputMessage instanceof ShutdownMessage) {
                            process((ShutdownMessage) inputMessage, target);
                        } else {
                            process(inputMessage, target);
                        }
                    } else {
                        AppUtils.sleep(50);
                    }
                }
            }
        } catch (Exception ex) {
            recordError(1, ex);
        }
    }
    
    protected synchronized void decrementActiveCount() {
        activeCount--;
    }
    
    protected synchronized int getActiveCountPlusQueueSize() {
        return activeCount + inQueue.size();
    }

    protected void process(Message inputMessage, SendMessageCallback target) {
        boolean unitOfWorkBoundaryReached = calculateUnitOfWorkLastMessage(inputMessage);
        /*
         * If unitOfWorkBoundaryReached, we might want to consider waiting to
         * send this until all other threads have finished processing to avoid
         * race conditions.
         */
        if (threadCount > 1) {
            this.componentRuntimeExecutor.execute(() -> processOnAnotherThread(inputMessage, unitOfWorkBoundaryReached, target));
        } else {
            processOnAnotherThread(inputMessage, unitOfWorkBoundaryReached, target);
        }
    }

    protected void processOnAnotherThread(Message inputMessage, boolean unitOfWorkBoundaryReached, SendMessageCallback target) {
        int threadNumber = ThreadUtils.getThreadNumber(threadCount);
        try {
            componentContext.getComponentStatistics().incrementInboundMessages(threadNumber);
            
            componentContext.getExecutionTracker().beforeHandle(threadNumber, componentContext);            

            IComponentRuntime componentRuntime = componentRuntimeByThread.get(threadNumber);

            Component component = componentContext.getFlowStep().getComponent();
            boolean logInput = component.getBoolean(AbstractComponentRuntime.LOG_INPUT, false);

            if (logInput) {
                logInput(inputMessage, target, unitOfWorkBoundaryReached);
            }
            target.setCurrentInputMessage(threadNumber, inputMessage);
            componentRuntime.handle(inputMessage, target, unitOfWorkBoundaryReached);

            if (unitOfWorkBoundaryReached && componentRuntime.getComponentDefintion().isAutoSendControlMessages()) {
                verifyAndSendControlMessageToTargets(target, inputMessage);
            }

            /*
             * Detect shutdown condition
             */
            if (startStep || (liveSourceStepIds.size() == 1 && liveSourceStepIds.contains(componentContext.getFlowStep().getId())
                    && getActiveCountPlusQueueSize() == 1)) {
                shutdown(threadNumber, target, false);
            }
        } catch (Exception ex) {
            recordError(ThreadUtils.getThreadNumber(threadCount), ex);
        } finally {
            componentContext.getExecutionTracker().afterHandle(threadNumber, componentContext, error);
            decrementActiveCount();
        }
    }

    protected synchronized boolean idle() {
        return activeCount <= 0;
    }

    protected void process(ShutdownMessage shutdownMessage, SendMessageCallback target) {
        cancelled = shutdownMessage.isCancelled();

        if (log.isDebugEnabled()) {
            log.debug("Processing shutdown message for " + componentContext.getFlowStep().getName()
                    + (cancelled ? ". The status was cancelled" : ""));
        }

        String fromStepId = shutdownMessage.getHeader().getOriginatingStepId();
        liveSourceStepIds.remove(fromStepId);

        /*
         * When all of the source step runtimes have been removed or when the
         * shutdown message comes from myself, then go ahead and shutdown
         */
        if (cancelled || fromStepId == null || liveSourceStepIds == null || liveSourceStepIds.size() == 0
                || fromStepId.equals(componentContext.getFlowStep().getId())) {
            shutdown(1, target, true);
        }
    }

    private void verifyAndSendControlMessageToTargets(ISendMessageCallback target, Message inputMessage) {
        for (StepRuntime targetRuntime : targetStepRuntimes) {
            if (!targetStepRuntimeUnitOfWorkSent.contains(targetRuntime.getComponentContext().getFlowStep().getId())) {
                log.info("Automatically sending a last unit of work message from " + componentContext.getFlowStep().getComponent().getName()
                        + " to " + targetRuntime.getComponentContext().getFlowStep().getComponent().getName()
                        + " because one was received but not sent forward.");
                target.sendControlMessage(inputMessage.getHeader());
            }
        }
    }

    protected boolean calculateUnitOfWorkLastMessage(Message inputMessage) {
        boolean lastMessage = true;
        if (inputMessage instanceof ControlMessage) {
            sourceStepRuntimeUnitOfWorkReceived.put(inputMessage.getHeader().getOriginatingStepId(), Boolean.TRUE);
        }
        Set<StepRuntime> activeRuntimes = new HashSet<>();
        for (StepRuntime sourceRuntime : sourceStepRuntimes) {
            boolean controlMessageReceived = sourceStepRuntimeUnitOfWorkReceived
                    .get(sourceRuntime.getComponentContext().getFlowStep().getId()) != null;
            lastMessage &= controlMessageReceived;
            if (!controlMessageReceived) {
                activeRuntimes.add(sourceRuntime);
            }
        }

        if (!lastMessage) {
            lastMessage = areStepRuntimesDead(activeRuntimes);
        }

        if (lastMessage) {
            // TODO figure out when/how to reset the last unit of work calc
            // sourceStepRuntimeUnitOfWorkReceived.clear();
        }
        return lastMessage;
    }

    protected boolean areStepRuntimesDead(Set<StepRuntime> runtimes) {
        for (StepRuntime stepRuntime : runtimes) {
            if (!isStepRuntimeDead(stepRuntime)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isStepRuntimeDead(StepRuntime stepRuntime) {
        if (stepRuntime.getContentMessagesSentCount() > 0
                || stepRuntime.getControlMessagesSentCount() > 0
                || !stepRuntime.isQueueEmpty() 
                || !stepRuntime.idle()) {
            return false;
        } else {
            List<StepRuntime> parentSteps = stepRuntime.getSourceStepRuntimes();
            for (StepRuntime parentStep : parentSteps) {
                boolean parentStepIsDead = parentStep.isStepRuntimeDead(parentStep);
                if (!parentStepIsDead && parentStep.getControlMessagesSentCount() == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private void stop(int threadNumber, IComponentRuntime componentRuntime) {
        try {
            componentRuntime.stop();
        } catch (Exception e) {
            recordError(threadNumber, e);
        }
    }

    private void shutdownTargets(StepRuntime targetStepRuntime) {
        try {
            targetStepRuntime.queue(new ShutdownMessage(componentContext.getFlowStep().getId(), cancelled));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void shutdown(int threadNumber, ISendMessageCallback target, boolean waitForShutdown) {
        shutdownThreads(waitForShutdown);

        if (log.isDebugEnabled()) {
            log.debug("Shutting down " + componentContext.getFlowStep().getName());
        }

        targetStepRuntimes.forEach(t -> shutdownTargets(t));
        componentRuntimeByThread.values().forEach(c -> stop(threadNumber, c));

        finished = true;
        running = false;

        recordFlowStepFinished();
    }

    private void shutdownThreads(boolean waitForShutdown) {
        if (this.componentRuntimeExecutor instanceof ExecutorService) {
            try {
                ExecutorService service = (ExecutorService) this.componentRuntimeExecutor;
                service.shutdown();
                while (waitForShutdown && !service.isTerminated()) {
                    service.awaitTermination(500, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                recordError(1, e);
            }
        }
    }

    private final void recordFlowStepFinished() {
        componentRuntimeByThread.keySet()
                .forEach(e -> componentContext.getExecutionTracker().flowStepFinished(e, componentContext, error, cancelled));
    }

    public void cancel() {
        shutdownThreads(true);
        if (!finished) {
            this.cancelled = true;
            recordFlowStepFinished();
        }
    }

    public void startRunning() {
        this.running = true;
    }

    public boolean isRunning() {
        return running;
    }
    
    public List<IComponentRuntime> getComponentRuntimes() {
        return new ArrayList<>(componentRuntimeByThread.values());
    }

    public void flowCompletedWithoutError() {
        componentRuntimeByThread.values().forEach(c -> flowCompletedWithoutError(c));
    }

    private void flowCompletedWithoutError(IComponentRuntime componentRuntime) {
        if (!cancelled) {
            try {
                componentRuntime.flowCompleted(cancelled);
            } catch (Exception ex) {
                recordError(1, ex);
                componentContext.getExecutionTracker().flowStepFailedOnComplete(componentContext, ex);
            }
        }
    }

    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors) {
        componentRuntimeByThread.values().forEach(c -> flowCompletedWithErrors(c, myError, allErrors));
    }

    private void flowCompletedWithErrors(IComponentRuntime componentRuntime, Throwable myError, List<Throwable> allErrors) {
        if (!cancelled) {
            try {
                componentRuntime.flowCompletedWithErrors(myError);
            } catch (Exception ex) {
                recordError(1, ex);
                componentContext.getExecutionTracker().flowStepFailedOnComplete(componentContext, ex);
            }
        }
    }

    public Throwable getError() {
        return error;
    }

    public ComponentContext getComponentContext() {
        return componentContext;
    }

    protected void logInput(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
        MessageHeader header = inputMessage.getHeader();

        int threadNumber = ThreadUtils.getThreadNumber(threadCount);

        String source = "ENTRY";
        try {
            source = componentContext.getManipulatedFlow().findFlowStepWithId(header.getOriginatingStepId()).getName();
        } catch (NullPointerException e) {
            // Do nothing allow "ENTRY" as the source.
        }

        componentContext.getExecutionTracker().log(threadNumber, LogLevel.INFO, componentContext,
                String.format("INPUT %s{sequenceNumber=%d,unitOfWorkBoundaryReached=%s,source='%s',headers=%s}",
                        inputMessage.getClass().getSimpleName(), header.getSequenceNumber(), unitOfWorkBoundaryReached, source, header));
        if (inputMessage instanceof EntityDataMessage) {
            EntityDataMessage message = (EntityDataMessage) inputMessage;
            if (componentContext.getFlowStep().getComponent().getInputModel() != null) {
                ArrayList<EntityData> payload = message.getPayload();
                for (EntityData entityData : payload) {
                    componentContext.getExecutionTracker().log(threadNumber, LogLevel.INFO, componentContext, String.format(
                            "INPUT Message Payload: %s", componentContext.getFlowStep().getComponent().toRow(entityData, true, true)));
                }
            }
        } else if (inputMessage instanceof TextMessage) {
            TextMessage message = (TextMessage) inputMessage;
            ArrayList<String> payload = message.getPayload();
            for (String string : payload) {
                componentContext.getExecutionTracker().log(threadNumber, LogLevel.INFO, componentContext,
                        String.format("INPUT Message Payload: %s", string));
            }
        }

    }

    protected void logOutput(Message outputMessage, String... targetFlowStepIds) {

        String targets = targetFlowStepIds != null && targetFlowStepIds.length > 0 ? Arrays.toString(targetFlowStepIds) : "[all]";
        int threadNumber = ThreadUtils.getThreadNumber(threadCount);

        MessageHeader header = outputMessage.getHeader();
        componentContext.getExecutionTracker().log(threadNumber, LogLevel.INFO, componentContext,
                String.format("OUTPUT %s{sequenceNumber=%d,headers=%s,targetsteps:%s}", outputMessage.getClass().getSimpleName(),
                        header.getSequenceNumber(), header, targets));
        if (outputMessage instanceof EntityDataMessage) {
            EntityDataMessage message = (EntityDataMessage) outputMessage;
            if (componentContext.getFlowStep().getComponent().getOutputModel() != null) {
                ArrayList<EntityData> payload = message.getPayload();
                for (EntityData entityData : payload) {
                    componentContext.getExecutionTracker().log(threadNumber, LogLevel.INFO, componentContext, String.format(
                            "OUTPUT Message Payload: %s", componentContext.getFlowStep().getComponent().toRow(entityData, true, false)));
                }
            }
        } else if (outputMessage instanceof TextMessage) {
            TextMessage message = (TextMessage) outputMessage;
            ArrayList<String> payload = message.getPayload();
            for (String string : payload) {
                componentContext.getExecutionTracker().log(threadNumber, LogLevel.INFO, componentContext,
                        String.format("OUTPUT Message Payload: %s", string));
            }
        }
    }

    class SendMessageCallback implements ISendMessageCallback {

        Map<Integer, Message> currentInputMessages = new HashMap<>();

        private void setCurrentInputMessage(int threadNumber, Message currentInputMessage) {
            currentInputMessages.put(threadNumber, currentInputMessage);
        }

        private Message createMessage(Message newMessage) {
            return createMessage(newMessage, null);
        }

        private Message createMessage(Message newMessage, Map<String, Serializable> headerSettings) {
            ComponentStatistics statistics = componentContext.getComponentStatistics();
            MessageHeader header = newMessage.getHeader();
            Message inputMessage = currentInputMessages.get(ThreadUtils.getThreadNumber(threadCount));
            if (inputMessage != null) {
                header.putAll(inputMessage.getHeader());
            }
            if (headerSettings != null) {
                header.putAll(headerSettings);
            }
            header.setSequenceNumber(statistics.getNumberOutboundMessages(ThreadUtils.getThreadNumber(threadCount)) + 1);
            return newMessage;
        }

        @SuppressWarnings("unchecked")
        private <T extends Serializable> T copy(T payload) {
            if (payload instanceof ArrayList) {
                ArrayList<?> old = (ArrayList<?>) payload;
                ArrayList<Object> copied = new ArrayList<>(old.size());
                for (Object object : old) {
                    if (object instanceof EntityData) {
                        object = ((EntityData) object).copy();
                    }
                    copied.add(object);
                }
                payload = (T) copied;
            } else if (payload instanceof byte[]) {
                payload = (T) ArrayUtils.clone((byte[]) payload);
            }
            return payload;
        }

        private void sendMessage(Message message, String... targetFlowStepIds) {
            ComponentStatistics statistics = componentContext.getComponentStatistics();
            int threadNumber = ThreadUtils.getThreadNumber(threadCount);
            statistics.incrementOutboundMessages(threadNumber);
            componentContext.getExecutionTracker().updateStatistics(threadNumber, componentContext);

            Component component = componentContext.getFlowStep().getComponent();
            boolean logOutput = component.getBoolean(AbstractComponentRuntime.LOG_OUTPUT, false);

            if (logOutput) {
                logOutput(message, targetFlowStepIds);
            }

            Collection<String> targetStepIds = targetFlowStepIds != null ? Arrays.asList(targetFlowStepIds) : Collections.emptyList();

            for (StepRuntime targetRuntime : targetStepRuntimes) {
                boolean forward = targetStepIds == null || targetStepIds.size() == 0
                        || targetStepIds.contains(targetRuntime.getComponentContext().getFlowStep().getId());
                if (forward) {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Sending " + message.getClass().getSimpleName() + " to "
                                    + targetRuntime.getComponentContext().getFlowStep().getName());
                        }
                        targetRuntime.queue(message);
                        if (message instanceof ControlMessage) {
                            targetStepRuntimeUnitOfWorkSent.add(targetRuntime.getComponentContext().getFlowStep().getId());
                        }
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
            sendMessage(createMessage(new ShutdownMessage(flowStep.getId(), cancel)));
        }

        @Override
        public void sendControlMessage() {
            sendControlMessage(null);
        }

        @Override
        public void sendControlMessage(Map<String, Serializable> messageHeaders, String... targetStepIds) {
            FlowStep flowStep = componentContext.getFlowStep();
            sendMessage(createMessage(new ControlMessage(flowStep.getId()), messageHeaders), targetStepIds);
            controlMessagesSentCount++;
        }

        @Override
        public void sendBinaryMessage(Map<String, Serializable> messageHeaders, byte[] payload, String... targetStepIds) {
            payload = copy(payload);
            FlowStep flowStep = componentContext.getFlowStep();
            sendMessage(createMessage(new BinaryMessage(flowStep.getId(), payload), messageHeaders), targetStepIds);
            contentMessagesSentCount++;
        }

        @Override
        public void sendEntityDataMessage(Map<String, Serializable> messageHeaders, ArrayList<EntityData> payload, String... targetStepIds) {
            payload = copy(payload);
            FlowStep flowStep = componentContext.getFlowStep();
            sendMessage(createMessage(new EntityDataMessage(flowStep.getId(), payload), messageHeaders), targetStepIds);
            contentMessagesSentCount++;
        }
        
        @Override
        public void sendTextMessage(Map<String, Serializable> messageHeaders, String payload, String... targetStepIds) {
            ArrayList<String> messages = new ArrayList<>();
            messages.add(payload);
            sendTextMessage(messageHeaders, messages, targetStepIds);
        }

        @Override
        public void sendTextMessage(Map<String, Serializable> messageHeaders, ArrayList<String> payload, String... targetStepIds) {
            payload = copy(payload);
            FlowStep flowStep = componentContext.getFlowStep();
            sendMessage(createMessage(new TextMessage(flowStep.getId(), payload), messageHeaders), targetStepIds);
            contentMessagesSentCount++;
        }

        @Override
        public void forward(Map<String, Serializable> messageHeaders, Message message) {
            if (message instanceof EntityDataMessage) {
                sendEntityDataMessage(messageHeaders, ((EntityDataMessage) message).getPayload());
            } else if (message instanceof TextMessage) {
                sendTextMessage(messageHeaders, ((TextMessage) message).getPayload());
            } else if (message instanceof BinaryMessage) {
                sendBinaryMessage(messageHeaders, ((BinaryMessage) message).getPayload());
            }
        }

        @Override
        public void forward(Message message) {
            forward(null, message);
        }
    }

    @Override
    public String toString() {
        return componentContext.getFlowStep().getName();
    }

}
