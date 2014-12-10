package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
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
    
    ComponentFlowNode componentFlowNode;

    public NodeRuntime(ComponentFlowNode componentFlowNode, IComponent component) {
        this.component = component;
        this.componentFlowNode = componentFlowNode;
        inQueue = new LinkedBlockingQueue<Message>(capacity);
    }
    
    public void setTargetNodeRuntimes(List<NodeRuntime> targetNodeRuntimes) {
        this.targetNodeRuntimes = targetNodeRuntimes;
    }

    protected void put(Message message) throws InterruptedException {
        inQueue.put(message);
    }
    
    public void start(IExecutionTracker tracker, IConnectionFactory connectionFactory) {
        component.start(tracker, connectionFactory, componentFlowNode);
    }

    @Override
    public void run() {
        try {
            MessageTarget target = new MessageTarget();
            while (running) {
                Message inputMessage = inQueue.take();
                if (!(inputMessage instanceof ShutdownMessage)) {
                    component.handle(inputMessage, target);
                } else {
                    running = false;
                }
            }
        } catch (Exception ex) {
            error = ex;
        }
    }

    public void stop() throws InterruptedException {
        this.running = false;
        this.inQueue.clear();
        this.inQueue.put(new ShutdownMessage());
    }

    class ShutdownMessage extends Message {

        private static final long serialVersionUID = 1L;

    }

    class MessageTarget implements IMessageTarget {

        @Override
        public void put(Message message) throws InterruptedException {
            for (NodeRuntime targetRuntime : targetNodeRuntimes) {
                targetRuntime.put(message);
            }
        }
    }
}
