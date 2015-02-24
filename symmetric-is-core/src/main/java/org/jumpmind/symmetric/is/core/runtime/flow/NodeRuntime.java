package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;

public class NodeRuntime implements Runnable {

    protected BlockingQueue<Message> inQueue;

    boolean running = true;

    // TODO make this a setting for component
    int capacity = 10000;

    Exception error;

    IComponent component;

    List<NodeRuntime> targetNodeRuntimes;

    List<NodeRuntime> sourceNodeRuntimes;

    public NodeRuntime(IComponent component) {
        this.component = component;
        inQueue = new LinkedBlockingQueue<Message>(capacity);
    }

    public boolean isStartNode() {
        return sourceNodeRuntimes == null || sourceNodeRuntimes.size() == 0;
    }

    public void setTargetNodeRuntimes(List<NodeRuntime> targetNodeRuntimes) {
        this.targetNodeRuntimes = targetNodeRuntimes;
    }

    public void setSourceNodeRuntimes(List<NodeRuntime> sourceNodeRuntimes) {
        this.sourceNodeRuntimes = sourceNodeRuntimes;
    }

    protected void put(Message message) throws InterruptedException {
        inQueue.put(message);
    }

    public void start(IExecutionTracker tracker, IConnectionFactory connectionFactory) {
        component.start(tracker, connectionFactory);
    }

    @Override
    public void run() {
        try {

            MessageTarget target = new MessageTarget();
            /*
             * if we are a start node (don't have any input links), we'll only
             * get a single message which is the start message sent by the flow
             * runtime to kick things off. If we have input links, we must loop
             * until we get a shutdown message from one of our sources
             */
            while (running) {
                Message inputMessage = inQueue.take();
                if (inputMessage instanceof ShutdownMessage) {
                    String fromNodeId = inputMessage.getHeader().getOriginatingNodeId();
                    removeSourceNodeRuntime(fromNodeId);
                    /*
                     * When all of the source node runtimes have been removed or
                     * when the shutdown message comes from myself, then go
                     * ahead and shutdown
                     */
                    if (fromNodeId == null || sourceNodeRuntimes == null
                            || sourceNodeRuntimes.size() == 0
                            || fromNodeId.equals(component.getComponentFlowNode().getId())) {
                        shutdown();
                    }
                } else {
                    component.handle(inputMessage, target);
                    if (isStartNode()) {
                        shutdown();
                    }
                }
            }
        } catch (Exception ex) {
            // TODO: notify the flow runtime that we have an error and let it
            // gracefully shut things down
            error = ex;
        }
    }

    private void removeSourceNodeRuntime(String nodeId) {
        if (sourceNodeRuntimes != null) {
            Iterator<NodeRuntime> it = sourceNodeRuntimes.iterator();
            while (it.hasNext()) {
                NodeRuntime sourceRuntime = (NodeRuntime) it.next();
                if (sourceRuntime.getComponent().getComponentFlowNode().getId().equals(nodeId)) {
                    it.remove();
                }
            }
        }
    }

    private void shutdown() throws InterruptedException {
        for (NodeRuntime targetNodeRuntime : targetNodeRuntimes) {
            targetNodeRuntime.put(new ShutdownMessage(component.getComponentFlowNode().getId()));
        }
        this.component.stop();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() throws InterruptedException {
        this.inQueue.clear();
        this.inQueue.put(new ShutdownMessage(component.getComponentFlowNode().getId()));
    }

    public IComponent getComponent() {
        return this.component;
    }

    class MessageTarget implements IMessageTarget {
        @Override
        public void put(Message message) {
            for (NodeRuntime targetRuntime : targetNodeRuntimes) {
                try {
                    targetRuntime.put(message);
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
