package org.jumpmind.symmetric.is.core.utils;

import java.util.Date;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ComponentEntitySetting;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.Resource;
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

    public static Flow createFlow(String name, Folder folder) {
        Flow flow = new Flow(folder);
        flow.setCreateBy("Test");
        flow.setCreateTime(new Date());
        flow.setFolderId(folder != null ? folder.getId() : null);
        flow.setId("FlowId");
        flow.setName("Flow");
        flow.setId(name);
        return flow;
    }

    public static void addStepToFlow(Flow flow, FlowStep step) {
        flow.getFlowSteps().add(step);
    }

    public static FlowStep createNoOpProcessorFlowStep(Flow flow, String name,
            Folder folder) {
        Component component = createComponent(NoOpProcessor.TYPE, false, null, null, null,
                null, null, (Setting[]) null);
        FlowStep step = new FlowStep(component);
        step.setFlowId(flow.getId());
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

    public static AgentDeployment createAgentDeployment(String name, Agent agent, Flow flow) {
        AgentDeployment deployment = new AgentDeployment(flow);
        deployment.setAgentId(agent.getId());
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

    public static Component createComponent(String type, boolean shared) {
        Component component = new Component();
        component.setCreateBy("Test");
        component.setCreateTime(new Date());
        component.setLastModifyBy("Test");
        component.setCreateTime(new Date());
        return component;
    }

    public static Component createComponent(String type, boolean shared, Resource resource,
            Model inputModelVersion, Model outputModelVersion,
            List<ComponentEntitySetting> entitySettings,
            List<ComponentAttributeSetting> attributeSettings, Setting... settings) {
        Component component = new Component(resource,
                inputModelVersion, outputModelVersion, entitySettings, attributeSettings, settings);
        component.setType(type);
        component.setShared(shared);
        component.setCreateBy("Test");
        component.setCreateTime(new Date());
        component.setId("Test");
        component.setLastModifyBy("Test");
        component.setLastModifyTime(new Date());
        return component;
    }
}
