package org.jumpmind.symmetric.is.core.runtime.flow;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.component.AbstractComponent;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentRuntime;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepRuntime implements Runnable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected BlockingQueue<Message> inQueue;

    boolean running = true;

    boolean cancelled = false;

    Throwable error;

    IComponentRuntime component;

    List<StepRuntime> targetStepRuntimes;

    List<StepRuntime> sourceStepRuntimes;

    IExecutionTracker executionTracker;

    public StepRuntime(IComponentRuntime component, IExecutionTracker tracker) {
        this.executionTracker = tracker;
        this.component = component;
        int capacity = component.getFlowStep().getComponent()
                .getInt(AbstractComponent.INBOUND_QUEUE_CAPACITY, 1000);
        inQueue = new LinkedBlockingQueue<Message>(capacity);
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
        inQueue.put(message);
    }

    public void start(IExecutionTracker tracker, IResourceFactory resourceFactory) {
        try {
            executionTracker.flowStepStarted(component);
            component.start(tracker);
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
        executionTracker.log(LogLevel.ERROR, component, msg);
        log.error("", ex);
    }

    @Override
    public void run() {
        try {

            MessageTarget target = new MessageTarget();
            /*
             * if we are a start step (don't have any input links), we'll only
             * get a single message which is the start message sent by the flow
             * runtime to kick things off. If we have input links, we must loop
             * until we get a shutdown message from one of our sources
             */
            while (running) {
                Message inputMessage = inQueue.take();
                if (inputMessage instanceof ShutdownMessage) {
                    ShutdownMessage shutdownMessage = (ShutdownMessage) inputMessage;

                    cancelled = shutdownMessage.isCancelled();

                    String fromStepId = inputMessage.getHeader().getOriginatingStepId();
                    removeSourceStepRuntime(fromStepId);
                    /*
                     * When all of the source step runtimes have been removed or
                     * when the shutdown message comes from myself, then go
                     * ahead and shutdown
                     */
                    if (cancelled || fromStepId == null || sourceStepRuntimes == null
                            || sourceStepRuntimes.size() == 0
                            || fromStepId.equals(component.getFlowStep().getId())) {
                        shutdown(target);
                    }
                } else {
                    try {
                        executionTracker.beforeHandle(component);
                        component.handle(inputMessage, target);
                    } catch (Exception ex) {
                        /*
                         * Record the error, but continue processing messages
                         */
                        recordError(ex);
                    } finally {
                        executionTracker.afterHandle(component, error);
                    }
                    if (isStartStep()) {
                        shutdown(target);
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

    private void removeSourceStepRuntime(String stepId) {
        if (sourceStepRuntimes != null) {
            Iterator<StepRuntime> it = sourceStepRuntimes.iterator();
            while (it.hasNext()) {
                StepRuntime sourceRuntime = (StepRuntime) it.next();
                if (sourceRuntime.getComponent().getFlowStep().getId().equals(stepId)) {
                    it.remove();
                }
            }
        }
    }

    private void shutdown(MessageTarget target) throws InterruptedException {
        this.component.lastMessageReceived(target);
        for (StepRuntime targetStepRuntime : targetStepRuntimes) {
            targetStepRuntime.queue(new ShutdownMessage(component.getFlowStep().getId(), cancelled));
        }
        this.component.stop();
        running = false;
        executionTracker.flowStepFinished(component, error, cancelled);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() throws InterruptedException {
        this.inQueue.clear();
        this.inQueue.put(new ShutdownMessage(component.getFlowStep().getId()));
    }

    public void flowCompletedWithoutError() {
        if (!cancelled) {
            try {
                component.flowCompleted();
            } catch (Exception ex) {
                recordError(ex);
                executionTracker.flowStepFailedOnComplete(component, ex);
            }
        }
    }

    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors) {
        if (!cancelled) {
            try {
                component.flowCompletedWithErrors(myError);
            } catch (Exception ex) {
                recordError(ex);
                executionTracker.flowStepFailedOnComplete(component, ex);
            }
        }
    }

    public IComponentRuntime getComponent() {
        return this.component;
    }

    public Throwable getError() {
        return error;
    }

    class MessageTarget implements IMessageTarget {
        @Override
        public void put(Message message) {
            Collection<String> targetStepIds = message.getHeader().getTargetStepIds();
            // clear out the target step id as we are done using it
            message.getHeader().setTargetStepIds(null);
            for (StepRuntime targetRuntime : targetStepRuntimes) {
                boolean forward = targetStepIds == null
                        || targetStepIds.size() == 0
                        || targetStepIds.contains(targetRuntime.getComponent().getFlowStep()
                                .getId());
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
    }

}
