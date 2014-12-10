package org.jumpmind.symmetric.is.core.runtime;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnection;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;

public class AgentDeploymentRuntime {

    AgentDeployment deployment;

    Map<ComponentFlowNode, IComponent> endpointRuntimes = new HashMap<ComponentFlowNode, IComponent>();

    Map<String, IConnection> connectionRuntimes = new HashMap<String, IConnection>();

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    IExecutionTracker executionTracker;

    public AgentDeploymentRuntime(AgentDeployment deployment, IComponentFactory componentFactory,
            IConnectionFactory connectionFactory, IExecutionTracker executionTracker) {
        this.executionTracker = executionTracker;
        this.deployment = deployment;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
    }

    public void start() {
        try {
            List<ComponentFlowNode> all = deployment.getComponentFlowVersion()
                    .getComponentFlowNodes();
            for (ComponentFlowNode node : all) {
                endpointRuntimes.put(node, componentFactory.create(node.getComponentVersion()));
//                endpointRuntimes.get(node).start(executionTracker, connectionFactory, node,
//                        new NodeChain(node));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        List<ComponentFlowNode> allNodes = deployment.getComponentFlowVersion()
                .getComponentFlowNodes();
        for (ComponentFlowNode node : allNodes) {
            endpointRuntimes.get(node).stop();
        }
        endpointRuntimes.clear();
    }

    @SuppressWarnings("unchecked")
    public <T extends IComponent> T getFirstComponentVersionRuntime(Class<T> type) {
        for (IComponent runtime : endpointRuntimes.values()) {
            if (runtime.getClass().isAssignableFrom(type)) {
                return (T) runtime;
            }
        }
        return null;
    }

    protected void doNext(ComponentFlowNode targetNode, Message message,
            ComponentFlowNode sourceNode) {
        validateMessageStructureMatchesInputModel(message, targetNode);
        IComponent runtime = (IComponent) endpointRuntimes.get(targetNode);
        //runtime.handle(message, sourceNode);
    }

    protected void validateMessageStructureMatchesInputModel(Message message,
            ComponentFlowNode targetNode) {

    }

    protected void validateOutputLink(ComponentFlowNode sourceNode, ComponentFlowNode targetNode) {

    }

    class NodeChain implements IComponentFlowChain {

        ComponentFlowNode sourceNode;

        public NodeChain(ComponentFlowNode sourceNode) {
            this.sourceNode = sourceNode;
        }

        @Override
        public void doNext(Message outputMessage) {
            boolean isFirst = false;
            MessageHeader header = outputMessage.getHeader();
            String executionId = header.getExecutionId();
            if (isBlank(executionId)) {
                executionId = UUID.randomUUID().toString();
                header.setExecutionId(executionId);
                executionTracker.beforeFlow(executionId);
                isFirst = true;
            }

            for (ComponentFlowNodeLink link : deployment.getComponentFlowVersion()
                    .getComponentFlowNodeLinks()) {
                if (link.getData().getSourceNodeId().equals(sourceNode.getData().getId())) {
                    ComponentFlowNode targetNode = deployment.getComponentFlowVersion()
                            .findComponentFlowNodeWithId(link.getData().getTargetNodeId());
                    validateOutputLink(sourceNode, targetNode);
                    executionTracker.beforeHandle(executionId, targetNode.getComponentVersion());
                    AgentDeploymentRuntime.this.doNext(targetNode, outputMessage, sourceNode);
                    executionTracker.afterHandle(executionId, targetNode.getComponentVersion());
                }
            }

            if (isFirst) {
                executionTracker.afterFlow(executionId);
            }
        }
    }

}
