package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.EntityRouter.Route;
import org.jumpmind.metl.core.util.NameValue;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(PowerMockRunner.class)
public class EntityRouterTest {

    Flow flow;
    
    FlowStep step;

    @Before
    public void setup() throws Exception {

        flow = TestUtils.createFlow("TestFlow", null);

        Setting[] settingData = createSettings();
        Component component = TestUtils.createComponent(EntityRouter.TYPE, false, null,
                createInputModel(), null, null, null, settingData);
        
        step = new FlowStep();
        step.setComponent(component);
        flow.getFlowSteps().add(step);

        FlowStep target1 = TestUtils.createNoOpProcessorFlowStep(flow, "Target 1", null);
        flow.getFlowSteps().add(target1);
        flow.getFlowStepLinks().add(new FlowStepLink(step.getId(), target1.getId()));

        FlowStep target2 = TestUtils.createNoOpProcessorFlowStep(flow, "Target 2", null);
        flow.getFlowSteps().add(target2);
        flow.getFlowStepLinks().add(new FlowStepLink(step.getId(), target2.getId()));

    }
    
    @Test
    public void testRouteToTarget1() {
        SendMessageCallback<ArrayList<EntityData>> target = route(new EntityData(new NameValue("tt1col1", "Route to 1")));
        assertEquals(1, target.getPayloadList().size());
        Collection<String> targetIds = target.getTargetStepIds().get(0);
        assertEquals(1, targetIds.size());
        assertEquals("Target 1", targetIds.iterator().next());
    }
    
    @Test
    public void testRouteToTarget1And2() {        
        SendMessageCallback<ArrayList<EntityData>> target = route(new EntityData(new NameValue("tt1col1", "Route to 1")),
                new EntityData(new NameValue("tt2colx", "Route to 2")));
        assertEquals(2, target.getPayloadList().size());
        Collection<String> targetIds = target.getTargetStepIds().get(0);
        assertEquals(1, targetIds.size());
        assertEquals("Target 1", targetIds.iterator().next());
        List<EntityData> datas = target.getPayloadList().get(0);
        assertEquals(1, datas.size());
        assertEquals("Route to 1", datas.get(0).get("tt1col1"));
        
        targetIds = target.getTargetStepIds().get(1);
        assertEquals(1, targetIds.size());
        assertEquals("Target 2", targetIds.iterator().next());
        datas = target.getPayloadList().get(1);
        assertEquals(1, datas.size());
        assertEquals("Route to 2", datas.get(0).get("tt2colx"));
    }
    
    protected SendMessageCallback<ArrayList<EntityData>> route(EntityData...data) {
        SendMessageCallback<ArrayList<EntityData>> target = new SendMessageCallback<ArrayList<EntityData>>();
        EntityRouter router = new EntityRouter();
        ComponentContext context = new ComponentContext(null, step, flow, new ExecutionTrackerNoOp(), null, null, null);
        router.start(context);
        Message inputMessage = new Message("");
        ArrayList<EntityData> datas = new ArrayList<EntityData>();
        for (EntityData entityData : data) {
            datas.add(entityData);
        }
        inputMessage.setPayload(datas);
        router.handle(inputMessage, target, true);
        return target;
    }

    private static Model createInputModel() {
        ModelEntity tt1 = new ModelEntity("tt1", "TEST_TABLE_1");
        tt1.addModelAttribute(new ModelAttribute("tt1col1", tt1.getId(), "COL1"));
        tt1.addModelAttribute(new ModelAttribute("tt1col2", tt1.getId(), "COL2"));
        tt1.addModelAttribute(new ModelAttribute("tt1col3", tt1.getId(), "COL3"));

        ModelEntity tt2 = new ModelEntity("tt2", "TEST_TABLE_2");
        tt2.addModelAttribute(new ModelAttribute("tt2colx", tt2.getId(), "COLX"));
        tt2.addModelAttribute(new ModelAttribute("tt2coly", tt2.getId(), "COLY"));
        tt2.addModelAttribute(new ModelAttribute("tt2colz", tt2.getId(), "COLZ"));

        Model modelVersion = new Model();
        modelVersion.getModelEntities().add(tt1);
        modelVersion.getModelEntities().add(tt2);

        return modelVersion;
    }

    private static Setting[] createSettings() throws Exception {
        Set<Route> routes = new HashSet<Route>();
        routes.add(new Route("TEST_TABLE_1.COL1=='Route to 1'", "Target 1"));
        routes.add(new Route("TEST_TABLE_2.COLX=='Route to 2'", "Target 2"));
        Setting[] settingData = new Setting[1];
        settingData[0] = new Setting(EntityRouter.SETTING_CONFIG,
                new ObjectMapper().writeValueAsString(routes));
        return settingData;
    }

}
