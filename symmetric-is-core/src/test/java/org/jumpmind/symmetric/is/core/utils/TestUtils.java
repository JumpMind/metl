package org.jumpmind.symmetric.is.core.utils;

import java.util.Date;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.component.NoOpProcessor;

public class TestUtils {
    
    public static Folder createFolder(String name) {    	
    	Folder data = new Folder();
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setName(name);
    	data.setId(name);
    	return data;
    }
    
    public static FlowVersion createFlowVersion(String name, Folder folder) {    	
    	Flow flow = createFlow(name, folder);
    	flow.setCreateBy("Test");
    	flow.setCreateTime(new Date());
    	flow.setId(name);
    	FlowVersion flowVersion = new FlowVersion(flow);
    	return flowVersion;
    }
    
    public static void addStepToFlow(FlowVersion flow, FlowStep step) {    	
    	flow.getFlowSteps().add(step);    	
    }
    
    public static FlowStep createNoOpProcessorFlowStep(FlowVersion flowVersion, 
    		String name, Folder folder) {
    	Component component = createComponent(NoOpProcessor.TYPE, false);
    	ComponentVersion componentVersion = createComponentVersion(component, null, (Setting[]) null);
    	FlowStep step = new FlowStep(componentVersion);
    	step.setFlowVersionId(flowVersion.getId());
    	//step.setComponentVersionId(componentVersion.getId());
    	step.setCreateBy("Test");
    	step.setCreateTime(new Date());
    	step.setId(name);
    	step.setLastModifyBy("Test");
    	step.setLastModifyTime(new Date());
    	return step;
    }
    
    public static Agent createAgent(String name, Folder folder) {    	
        Agent agent = new Agent(folder);
    	agent.setCreateBy("Test");
    	agent.setCreateTime(new Date());
    	agent.setFolderId(folder.getId());
    	agent.setHeartbeatTime(new Date());
    	agent.setHost("localhost");
    	agent.setId(name);
    	agent.setLastModifyBy("Test");
    	agent.setLastModifyTime(new Date());
    	agent.setLastStartTime(new Date());
    	agent.setName(name);
    	return agent;
    }
    
    public static AgentDeployment createAgentDeployment(String name, Agent agent, FlowVersion flow) {    	
        AgentDeployment deployment = new AgentDeployment(flow);
    	deployment.setAgentId(agent.getId());
    	//deployment.setFlowVersionId(flow.getId());
    	deployment.setCreateBy("Test");
    	deployment.setCreateTime(new Date());
    	deployment.setId(name);
    	deployment.setLastModifyBy("Test");
    	deployment.setLastModifyTime(new Date());
    	return deployment;    			
    }

    public static FlowStepLink createComponentLink(FlowStep srcStep, FlowStep destStep) {    	
    	FlowStepLink link = new FlowStepLink(srcStep.getId(), destStep.getId());
    	return link;
    }
    
    private static Flow createFlow(String name, Folder folder) {    	
        Flow flow = new Flow(folder);
    	flow.setCreateBy("Test");
    	flow.setCreateTime(new Date());
    	flow.setFolderId(folder.getId());
    	flow.setId("FlowId");
    	flow.setName("Flow");
    	return flow;
    }
    
    public static Component createComponent(String type, boolean shared) {
    	Component component = new Component();
    	component.setType(type);
    	component.setShared(shared);
    	component.setCreateBy("Test");
    	component.setCreateTime(new Date());
    	component.setLastModifyBy("Test");
    	component.setCreateTime(new Date());    	
    	return component;
    }
    
	public static ComponentVersion createComponentVersion(Component component,
			Resource resource, Setting... settings) {
        ComponentVersion componentVersion = new ComponentVersion(component,
                resource, null, null, settings);
        //componentVersion.setComponentId(component.getId());
        componentVersion.setCreateBy("Test");
        componentVersion.setCreateTime(new Date());
        componentVersion.setId("Test");
        componentVersion.setLastModifyBy("Test");
        componentVersion.setLastModifyTime(new Date());
		// TODO: allow passing in of a model
        componentVersion.setInputModelVersiondId(null);
        componentVersion.setOutputModelVersionId(null);
		return componentVersion;
	}

}
