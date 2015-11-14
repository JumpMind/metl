/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.runtime.component.ComponentRuntimeFromXMLFactory;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.ResourceFactory;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FlowRuntimeTest {

    IDatabasePlatform platform;
    IComponentRuntimeFactory componentFactory;
    IResourceFactory resourceFactory;
    ExecutorService threadService;
    
    Folder folder;
    Agent agent;
    
    @Before
    public void setup() throws Exception {
    	
    	componentFactory = new ComponentRuntimeFromXMLFactory();
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
    	
    	Flow flow = createSimpleTwoStepNoOpFlow(folder);
    	AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);	
    	FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, resourceFactory, 
    			 threadService, null, null);
    	flowRuntime.start("", new HashMap<String, IResourceRuntime>(), agent, new ArrayList<Notification>(), new HashMap<String, String>());
    	flowRuntime.waitForFlowCompletion();
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step").getNumberInboundMessages(1));
    	Assert.assertEquals(2, flowRuntime.getComponentStatistics("Target Step").getNumberInboundMessages(1));
    }
    
    @Test
    public void singleSrcToTwoTarget() throws Exception {
    	Flow flow = createSrcToTwoTargetFlow(folder);
    	AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);
    	FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, resourceFactory, 
    			 threadService, null, null);
    	flowRuntime.start("", new HashMap<String, IResourceRuntime>(), agent, new ArrayList<Notification>(), new HashMap<String, String>());
    	flowRuntime.waitForFlowCompletion();
    	Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step").getNumberInboundMessages(1));
    	Assert.assertEquals(2, flowRuntime.getComponentStatistics("Target Step 1").getNumberInboundMessages(1));
    	Assert.assertEquals(2, flowRuntime.getComponentStatistics("Target Step 2").getNumberInboundMessages(1));    	
    }
    
    @Test
    public void twoSrcOneTarget() throws Exception {
        Flow flow = createTwoSrcToOneTargetFlow(folder);
        AgentDeployment deployment = TestUtils.createAgentDeployment("TestAgentDeploy", agent, flow);
        FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory, resourceFactory
                , threadService, null, null);
        flowRuntime.start("", new HashMap<String, IResourceRuntime>(), agent, new ArrayList<Notification>(), new HashMap<String, String>());
        flowRuntime.waitForFlowCompletion();
        Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step 1").getNumberInboundMessages(1));
        Assert.assertEquals(1, flowRuntime.getComponentStatistics("Src Step 2").getNumberInboundMessages(1));
        Assert.assertEquals(4, flowRuntime.getComponentStatistics("Target Step").getNumberInboundMessages(1));     
    }
    
    private Flow createSimpleTwoStepNoOpFlow(Folder folder) {

    	Flow flow = TestUtils.createFlow("TestFlow", folder);
    	FlowStep srcNoOpStep = TestUtils.createNoOpProcessorFlowStep(flow, "Src Step", folder);
    	FlowStep targetNoOpStep = TestUtils.createNoOpProcessorFlowStep(flow, "Target Step", folder);
    	flow.getFlowStepLinks().add(TestUtils.createComponentLink(srcNoOpStep, targetNoOpStep));
    	TestUtils.addStepToFlow(flow, srcNoOpStep);
    	TestUtils.addStepToFlow(flow, targetNoOpStep);

    	return flow;
    }

    private Flow createSrcToTwoTargetFlow(Folder folder) {

    	Flow flow = TestUtils.createFlow("TestFlow", folder);
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
    
    private Flow createTwoSrcToOneTargetFlow(Folder folder) {

        Flow flow = TestUtils.createFlow("TestFlow", folder);
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
