package org.jumpmind.metl.core.runtime.flow;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.runtime.IExecutionTracker;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.jumpmind.metl.core.runtime.component.ComponentStatistics;
import org.jumpmind.metl.core.runtime.component.IComponentRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StepRuntime implements Runnable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected BlockingQueue<Message> inQueue;

    boolean running = false;

    boolean cancelled = false;
    
    boolean unitOfWorkLastMessage = false;

    Throwable error;

    IComponentRuntime componentRuntime;

    List<StepRuntime> targetStepRuntimes;

    List<StepRuntime> sourceStepRuntimes;
    
    Map<String, Message> sourceStepRuntimeMessages;

    ComponentContext componentContext;

    FlowRuntime flowRuntime;

    public StepRuntime(IComponentRuntime componentRuntime, ComponentContext componentContext, FlowRuntime flowRuntime) {
        this.flowRuntime = flowRuntime;
        this.componentContext = componentContext;
        this.componentRuntime = componentRuntime;
        int capacity = componentContext.getFlowStep().getComponent().getInt(AbstractComponentRuntime.INBOUND_QUEUE_CAPACITY, 1000);
        inQueue = new LinkedBlockingQueue<Message>(capacity);
        sourceStepRuntimeMessages = new HashMap<String, Message>();
    }

    public boolean isUnitOfWorkLastMessage() {
    	return unitOfWorkLastMessage;
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
            MessageTarget target = new MessageTarget();
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
                        	unitOfWorkLastMessage = calculateUnitOfWorkLastMessage(inputMessage);
                            componentContext.getExecutionTracker().beforeHandle(componentContext);
                            componentRuntime.handle(inputMessage, target, unitOfWorkLastMessage);
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
    		for (StepRuntime sourceRuntime:sourceStepRuntimes) {
    			if (sourceStepRuntimeMessages.get(sourceRuntime.getComponentContext().getFlowStep().getId()) == null &&
    					sourceRuntime.getComponentContext().getFlowStep().getId() != inputMessage.getHeader().getOriginatingStepId()) {
    				if (sourceStepRuntimeMessages.get(inputMessage.getHeader().getOriginatingStepId()) == null) {
    					sourceStepRuntimeMessages.put(inputMessage.getHeader().getOriginatingStepId(), inputMessage);
    				} else {
    					//TODO: in this case we received more than one unit of work last
    					//message from one source before we received from the other source
    					//we should not process this message, but hold it until we pair up
    					//last unit of work messages from all sources
    				}
    				lastMessage=false;
    				break;
    			}
    		}
    	}
    	if (lastMessage) {
    		sourceStepRuntimeMessages.clear();
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

    private void shutdown(MessageTarget target) throws InterruptedException {
        try {
            this.componentRuntime.lastMessageReceived(target);
        } catch (Exception e) {
            recordError(e);
        }
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

    class MessageTarget implements IMessageTarget {
        @Override
        public void put(Message message) {
            Collection<String> targetStepIds = message.getHeader().getTargetStepIds();
            // clear out the target step id as we are done using it
            message.getHeader().setTargetStepIds(null);
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
    }

    @Override
    public String toString() {
        return componentContext.getFlowStep().getName();
    }

}
