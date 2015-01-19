package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.runtime.ExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FlowRuntimeTest {

    IDatabasePlatform platform;
    IComponentFactory componentFactory;
    IConnectionFactory connectionFactory;
    ExecutorService threadService;
    
    Folder folder;
    Agent agent;
    
    @Before
    public void setup() throws Exception {
    	
    	componentFactory = new ComponentFactory();
    	connectionFactory = new ConnectionFactory();
    	threadService = Executors.newFixedThreadPool(5);
    	
    	folder = TestUtils.createFolder("Test Folder");
    	agent = TestUtils.createAgent("TestAgent", folder);
    }
    
    @After
    public void tearDown() throws Exception {
        threadService.shutdown();
    }

    @Test
    public void simpleTwoNodeNoOp() throws Exception {
    	
    	ComponentFlowVersion flow = createSimpleTwoNodeNoOpFlow(folder);
    	AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);	
    	FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, connectionFactory, 
    			new ExecutionTracker(deployment), threadService);
    	flowRuntime.start();
    	flowRuntime.stop();
    	flowRuntime.waitForFlowCompletion();
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Node").getNumberInboundMessages());
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Target Node").getNumberInboundMessages());
    }
    
    @Test
    public void singleSrcToTwoTarget() throws Exception {
    	ComponentFlowVersion flow = createSrcToTwoTargetFlow(folder);
    	AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);
    	FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, connectionFactory, 
    			new ExecutionTracker(deployment), threadService);
    	flowRuntime.start();
    	flowRuntime.stop();
    	flowRuntime.waitForFlowCompletion();
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Node").getNumberInboundMessages());
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Target Node 1").getNumberInboundMessages());
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Target Node 2").getNumberInboundMessages());    	
    }
    
    private ComponentFlowVersion createSimpleTwoNodeNoOpFlow(Folder folder) {

    	ComponentFlowVersion flow = TestUtils.createFlow("TestFlow", folder);
    	ComponentFlowNode srcNoOpNode = TestUtils.createNoOpProcessorComponentFlowNode(flow, "Src Node", folder);
    	ComponentFlowNode targetNoOpNode = TestUtils.createNoOpProcessorComponentFlowNode(flow, "Target Node", folder);
    	flow.getComponentFlowNodeLinks().add(TestUtils.createComponentLink(srcNoOpNode, targetNoOpNode));
    	TestUtils.addNodeToComponentFlow(flow, srcNoOpNode);
    	TestUtils.addNodeToComponentFlow(flow, targetNoOpNode);

    	return flow;
    }

    private ComponentFlowVersion createSrcToTwoTargetFlow(Folder folder) {

    	ComponentFlowVersion flow = TestUtils.createFlow("TestFlow", folder);
    	ComponentFlowNode srcNoOpNode = TestUtils.createNoOpProcessorComponentFlowNode(flow, "Src Node", folder);
    	ComponentFlowNode targetNoOpNode1 = TestUtils.createNoOpProcessorComponentFlowNode(flow, "Target Node 1", folder);
    	ComponentFlowNode targetNoOpNode2 = TestUtils.createNoOpProcessorComponentFlowNode(flow, "Target Node 2", folder);    	
    	flow.getComponentFlowNodeLinks().add(TestUtils.createComponentLink(srcNoOpNode, targetNoOpNode1));
    	flow.getComponentFlowNodeLinks().add(TestUtils.createComponentLink(srcNoOpNode, targetNoOpNode2));
    	TestUtils.addNodeToComponentFlow(flow, srcNoOpNode);
    	TestUtils.addNodeToComponentFlow(flow, targetNoOpNode1);
    	TestUtils.addNodeToComponentFlow(flow, targetNoOpNode2);
    	
    	return flow;   	
    } 
}
