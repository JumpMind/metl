package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentStatistics;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnection;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;

public class FlowRuntime {

    AgentDeployment deployment;

    Map<ComponentFlowNode, IComponent> endpointRuntimes = new HashMap<ComponentFlowNode, IComponent>();

    Map<String, IConnection> connectionRuntimes = new HashMap<String, IConnection>();

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    IExecutionTracker executionTracker;

    ExecutorService threadService;

    Map<String, NodeRuntime> nodeRuntimes;

    public FlowRuntime(AgentDeployment deployment, IComponentFactory componentFactory,
            IConnectionFactory connectionFactory, IExecutionTracker executionTracker,
            ExecutorService threadService) {
        this.executionTracker = executionTracker;
        this.deployment = deployment;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
        this.threadService = threadService;
    }

    public void start() throws InterruptedException {
    	
        this.nodeRuntimes = new HashMap<String, NodeRuntime>();
        ComponentFlowVersion componentFlowVersion = deployment.getComponentFlowVersion();
        List<ComponentFlowNode> nodes = componentFlowVersion.getComponentFlowNodes();
        
        //create a node runtime for every component in the flow
        for (ComponentFlowNode componentFlowNode : nodes) {
            NodeRuntime nodeRuntime = new NodeRuntime(
            		componentFactory.create(componentFlowNode));
            nodeRuntimes.put(componentFlowNode.getId(), nodeRuntime);
        }

        //for each node runtime, set their list of target node runtimes
        for (String nodeId : nodeRuntimes.keySet()) {
            List<NodeRuntime> targetNodeRuntimes = new ArrayList<NodeRuntime>();
            List<ComponentFlowNodeLink> links = componentFlowVersion.getComponentFlowNodeLinks();
            for (ComponentFlowNodeLink componentFlowNodeLink : links) {
                if (nodeId.equals(componentFlowNodeLink.getData().getSourceNodeId())) {
                    targetNodeRuntimes.add(nodeRuntimes.get(componentFlowNodeLink.getData()
                            .getTargetNodeId()));
                }
            }
            nodeRuntimes.get(nodeId).setTargetNodeRuntimes(targetNodeRuntimes);
        }

        List<NodeRuntime> startNodes = findStartNodes();
        for (NodeRuntime nodeRuntime : startNodes) {
        	nodeRuntime.setStartNode(true);
        }
        
        //each node is started as a thread
        for (NodeRuntime nodeRuntime : nodeRuntimes.values()) {
            threadService.execute(nodeRuntime);
        }

        //start up each node runtime
        for (NodeRuntime nodeRuntime : nodeRuntimes.values()) {
            nodeRuntime.start(executionTracker, connectionFactory);
        }
        
        Message startMessage = new Message();
        // TODO set parameters on start message
        //for each start node (node that has no input msgs), send a start message to that node
        for (NodeRuntime nodeRuntime : startNodes) {
            nodeRuntime.put(startMessage);
        }
    }

    /* created for unit testing only to keep main thread alive until child threads complete */
    public void waitForFlowCompletion() throws InterruptedException {
    	threadService.shutdown();
    	//TODO: parameterize timeout
    	threadService.awaitTermination(60, TimeUnit.MINUTES);    	
    }
    
    protected List<NodeRuntime> findStartNodes() {
        List<NodeRuntime> starterNodes = new ArrayList<NodeRuntime>();
        for (String nodeId : nodeRuntimes.keySet()) {
            List<ComponentFlowNodeLink> links = deployment.getComponentFlowVersion()
                    .getComponentFlowNodeLinks();
            boolean isTargetNode = false;
            for (ComponentFlowNodeLink componentFlowNodeLink : links) {
                if (nodeId.equals(componentFlowNodeLink.getData().getTargetNodeId())) {
                    isTargetNode = true;
                }
            }
            
            if (!isTargetNode) {
                starterNodes.add(nodeRuntimes.get(nodeId));
            }
        }
        return starterNodes;
    }

    public void stop() {

    }
    
    public ComponentStatistics getComponentStatistics(String componentFlowNodeId) {
    	return nodeRuntimes.get(componentFlowNodeId).getComponent().getComponentStatistcics();
    }
}
