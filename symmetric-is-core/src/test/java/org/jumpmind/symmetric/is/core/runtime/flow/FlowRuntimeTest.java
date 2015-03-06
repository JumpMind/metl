package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.runtime.ExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceFactory;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FlowRuntimeTest {

    IDatabasePlatform platform;
    IComponentFactory componentFactory;
    IResourceFactory resourceFactory;
    ExecutorService threadService;
    
    Folder folder;
    Agent agent;
    
    @Before
    public void setup() throws Exception {
    	
    	componentFactory = new ComponentFactory();
    	resourceFactory = new ResourceFactory();
    	threadService = Executors.newFixedThreadPool(5);
    	
    	folder = TestUtils.createFolder("Test Folder");
    	agent = TestUtils.createAgent("TestAgent", folder);
    }
    
    @After
    public void tearDown() throws Exception {
        threadService.shutdown();
    }

    @Test
    public void simpleTwoStepNoOp() throws Exception {
    	
    	FlowVersion flow = createSimpleTwoStepNoOpFlow(folder);
    	AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);	
    	FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, resourceFactory, 
    			new ExecutionTracker(deployment), threadService);
    	flowRuntime.start();
    	flowRuntime.waitForFlowCompletion();
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step").getNumberInboundMessages());
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Target Step").getNumberInboundMessages());
    }
    
    @Test
    public void singleSrcToTwoTarget() throws Exception {
    	FlowVersion flow = createSrcToTwoTargetFlow(folder);
    	AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);
    	FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, resourceFactory, 
    			new ExecutionTracker(deployment), threadService);
    	flowRuntime.start();
    	flowRuntime.waitForFlowCompletion();
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step").getNumberInboundMessages());
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Target Step 1").getNumberInboundMessages());
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Target Step 2").getNumberInboundMessages());    	
    }
    
    @Test
    public void twoSrcOneTarget() throws Exception {
        FlowVersion flow = createTwoSrcToOneTargetFlow(folder);
        AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);
        FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, resourceFactory, 
                new ExecutionTracker(deployment), threadService);
        flowRuntime.start();
        flowRuntime.waitForFlowCompletion();
        Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step 1").getNumberInboundMessages());
        Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step 2").getNumberInboundMessages());
        Assert.assertEquals(2, flowRuntime.getComponentStatistics("Target Step").getNumberInboundMessages());     
    }
    
    private FlowVersion createSimpleTwoStepNoOpFlow(Folder folder) {

    	FlowVersion flow = TestUtils.createFlowVersion("TestFlow", folder);
    	FlowStep srcNoOpStep = TestUtils.createNoOpProcessorFlowStep(flow, "Src Step", folder);
    	FlowStep targetNoOpStep = TestUtils.createNoOpProcessorFlowStep(flow, "Target Step", folder);
    	flow.getFlowStepLinks().add(TestUtils.createComponentLink(srcNoOpStep, targetNoOpStep));
    	TestUtils.addStepToFlow(flow, srcNoOpStep);
    	TestUtils.addStepToFlow(flow, targetNoOpStep);

    	return flow;
    }

    private FlowVersion createSrcToTwoTargetFlow(Folder folder) {

    	FlowVersion flow = TestUtils.createFlowVersion("TestFlow", folder);
    	FlowStep srcNoOpStep = TestUtils.createNoOpProcessorFlowStep(flow, "Src Step", folder);
    	FlowStep targetNoOpStep1 = TestUtils.createNoOpProcessorFlowStep(flow, "Target Step 1", folder);
    	FlowStep targetNoOpStep2 = TestUtils.createNoOpProcessorFlowStep(flow, "Target Step 2", folder);    	
    	flow.getFlowStepLinks().add(TestUtils.createComponentLink(srcNoOpStep, targetNoOpStep1));
    	flow.getFlowStepLinks().add(TestUtils.createComponentLink(srcNoOpStep, targetNoOpStep2));
    	TestUtils.addStepToFlow(flow, srcNoOpStep);
    	TestUtils.addStepToFlow(flow, targetNoOpStep1);
    	TestUtils.addStepToFlow(flow, targetNoOpStep2);
    	
    	return flow;   	
    } 
    
    private FlowVersion createTwoSrcToOneTargetFlow(Folder folder) {

        FlowVersion flow = TestUtils.createFlowVersion("TestFlow", folder);
        FlowStep srcNoOpStep1 = TestUtils.createNoOpProcessorFlowStep(flow, "Src Step 1", folder);
        FlowStep srcNoOpStep2 = TestUtils.createNoOpProcessorFlowStep(flow, "Src Step 2", folder);
        FlowStep targetNoOpStep = TestUtils.createNoOpProcessorFlowStep(flow, "Target Step", folder);
        flow.getFlowStepLinks().add(TestUtils.createComponentLink(srcNoOpStep1, targetNoOpStep));
        flow.getFlowStepLinks().add(TestUtils.createComponentLink(srcNoOpStep2, targetNoOpStep));
        TestUtils.addStepToFlow(flow, srcNoOpStep1);
        TestUtils.addStepToFlow(flow, srcNoOpStep2);
        TestUtils.addStepToFlow(flow, targetNoOpStep);
        
        return flow;    
    } 
}
