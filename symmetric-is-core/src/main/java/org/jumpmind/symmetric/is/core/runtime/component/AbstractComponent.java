package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnection;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;

abstract public class AbstractComponent extends AbstractRuntimeObject implements IComponent {

    protected ComponentFlowNode componentNode;    
    protected IConnection connection;
    protected IConnectionFactory connectionFactory;
    protected IExecutionTracker executionTracker;
    protected ComponentStatistics componentStatistics;

    public void start(IExecutionTracker executionTracker, IConnectionFactory connectionFactory) {
        this.componentStatistics = new ComponentStatistics();
    	this.executionTracker = executionTracker;
        this.connectionFactory = connectionFactory;

        Connection connection = componentNode.getComponentVersion().getConnection();
        if (connection != null) {
            try {
                this.connection = connectionFactory.create(connection);
                this.connection.start(connection);
                //TODO: connection.start gets called twice here...  Once during the create and again in the start after it.
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        if (this.connection != null) {
            this.connection.stop();
        }
    }
    
    public ComponentStatistics getComponentStatistics() {
    	return componentStatistics;
    }
    
    public ComponentFlowNode getComponentFlowNode() {
    	return this.componentNode;
    }
    
    public void setComponentFlowNode(ComponentFlowNode componentNode) {
    	this.componentNode = componentNode;
    }

}
