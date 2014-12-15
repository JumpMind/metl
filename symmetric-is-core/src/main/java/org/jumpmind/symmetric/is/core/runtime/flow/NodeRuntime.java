package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;

public class NodeRuntime implements Runnable {

    protected BlockingQueue<Message> inQueue;

    boolean running = true;

    boolean startNode = false;
    
	// TODO make this a setting for component
    int capacity = 10000;

    Exception error;

    IComponent component;

    List<NodeRuntime> targetNodeRuntimes;
    
    //TODO: can we get rid of this in lieu of the component itself?
    ComponentFlowNode componentFlowNode;

    public NodeRuntime(ComponentFlowNode componentFlowNode, IComponent component) {
        this.component = component;
        this.componentFlowNode = componentFlowNode;
        inQueue = new LinkedBlockingQueue<Message>(capacity);
    }

    public boolean isStartNode() {
		return startNode;
	}

	public void setStartNode(boolean startNode) {
		this.startNode = startNode;
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
        	/* if we are a start node (don't have any input links), we'll only get 
        	a single message which is the start message sent by the flow runtime
        	to kick things off.  If we have input links, we must loop until we get
        	a shutdown message from one of our sources */
        	if (startNode) {
        		component.handle(inQueue.take(), target);
        	} else {
        		while (running) {
                    Message inputMessage = inQueue.take();
                    if (!(inputMessage instanceof ShutdownMessage)) {
                        component.handle(inputMessage, target);
                    } else {
                    	running = false;
                    }
        		}
        	}        	
        } catch (Exception ex) {
        	//TODO: notify te flowruntime that we have an error and let it gracefully shut things down
            error = ex;
        }
    }

    public void stop() throws InterruptedException {
        this.inQueue.clear();
        this.inQueue.put(new ShutdownMessage());
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
