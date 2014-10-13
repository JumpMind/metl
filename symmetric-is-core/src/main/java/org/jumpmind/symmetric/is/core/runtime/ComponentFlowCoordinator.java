package org.jumpmind.symmetric.is.core.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnection;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;

public class ComponentFlowCoordinator {

    ComponentFlowVersion flow;

    Map<ComponentFlowNode, IComponent> endpointRuntimes = new HashMap<ComponentFlowNode, IComponent>();

    Map<String, IConnection> connectionRuntimes = new HashMap<String, IConnection>();

    List<IComponentFlowListener> runtimeListeners = new ArrayList<IComponentFlowListener>();

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    public ComponentFlowCoordinator(ComponentFlowVersion flow, IComponentFactory componentFactory,
            IConnectionFactory connectionFactory) {
        this.flow = flow;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
    }

    public void addComponentVersionRuntimeListener(IComponentFlowListener listener) {
        this.runtimeListeners.add(listener);
    }

    public void start() {
        try {
            List<ComponentFlowNode> all = flow.getComponentFlowNodes();
            for (ComponentFlowNode node : all) {
                endpointRuntimes.put(node, componentFactory.create(node.getComponentVersion()));
                endpointRuntimes.get(node).start(connectionFactory, node, new NodeChain(node));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        List<ComponentFlowNode> allNodes = flow.getComponentFlowNodes();
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
        // TODO execute in parallel/async if configured
        validateMessageStructureMatchesInputModel(message, targetNode);
        IComponent runtime = (IComponent) endpointRuntimes.get(targetNode);
        for (IComponentFlowListener listener : runtimeListeners) {
            listener.beforeHandle(runtime, message, sourceNode);
        }
        runtime.handle(message, sourceNode);
        for (IComponentFlowListener listener : runtimeListeners) {
            listener.afterHandle(runtime, message, sourceNode);
        }
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
            for (ComponentFlowNode targetNode : sourceNode.getOutputLinks()) {
                validateOutputLink(sourceNode, targetNode);
                ComponentFlowCoordinator.this.doNext(targetNode, outputMessage, sourceNode);
            }
        }

    }

}
