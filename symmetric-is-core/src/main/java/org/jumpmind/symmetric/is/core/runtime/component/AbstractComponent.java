package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.IComponent;
import org.jumpmind.symmetric.is.core.runtime.IComponentFlowChain;
import org.jumpmind.symmetric.is.core.runtime.IConnection;
import org.jumpmind.symmetric.is.core.runtime.Message;

abstract public class AbstractComponent extends AbstractRuntimeObject implements IComponent {

    protected IComponentFlowChain chain;
    
    protected ComponentFlowNode componentNode;
    
    protected IConnection connection;
    
    protected ConnectionFactory connectionFactory;

    @Override
    public void start(ConnectionFactory connectionFactory, ComponentFlowNode componentNode, IComponentFlowChain chain) {
        this.componentNode = componentNode;
        this.chain = chain;
        this.connectionFactory = connectionFactory;

        Connection connection = componentNode.getComponentVersion().getConnection();
        if (connection != null) {
            try {
                this.connection = connectionFactory.create(connection);
                this.connection.start(connection);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void stop() {
        if (this.connection != null) {
            this.connection.stop();
        }
    }

    @Override
    public void handle(Message inputMessage,
            ComponentFlowNode inputLink) {
        chain.doNext(inputMessage);
    }

}
