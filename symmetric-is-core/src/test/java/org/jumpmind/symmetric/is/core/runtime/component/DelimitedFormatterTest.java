package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.ExecutionTrackerLogger;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DelimitedFormatterTest {

    private static FlowStep delimitedFormatterFlowStep;
    
    @BeforeClass
    public static void setup() throws Exception {
        delimitedFormatterFlowStep = createDelimitedFormatterFlowStep();
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testDelimitedFormatterFromSingleContentMsg() throws Exception {

        IExecutionTracker executionTracker = new ExecutionTrackerLogger(new AgentDeployment(new Flow()));
        DelimitedFormatter delimitedFormatter = new DelimitedFormatter();
        delimitedFormatter.init(delimitedFormatterFlowStep, null);
        delimitedFormatter.start(executionTracker, null);        
        Message message = createInboundMessage();        
        MessageTarget msgTarget = new MessageTarget();
        delimitedFormatter.handle("test", message, msgTarget);

        assertEquals(1, msgTarget.getTargetMessageCount());
        ArrayList<EntityData> payload = msgTarget.getMessage(0).getPayload();
        assertEquals(1,payload.size());
        assertEquals("tt1col2_value|tt1col1_value|tt2col2_value|tt2col3_value|tt1col3_value|tt2col1_value", payload.get(0));
    }

    private static Message createInboundMessage() {
        
        Message message = new Message("fake step id");
        ArrayList<EntityData> inboundPayload = new ArrayList<EntityData>();
        EntityData entityData = new EntityData();
        entityData.put("tt1col1", "tt1col1_value");
        entityData.put("tt1col2", "tt1col2_value");
        entityData.put("tt1col3", "tt1col3_value");
        entityData.put("tt2col1", "tt2col1_value");
        entityData.put("tt2col2", "tt2col2_value");
        entityData.put("tt2col3", "tt2col3_value");
        
        inboundPayload.add(entityData);
        message.setPayload(inboundPayload);
        
        return message;
    }
    
    private static FlowStep createDelimitedFormatterFlowStep() {

        Folder folder = TestUtils.createFolder("Test Folder");
        Flow flow = TestUtils.createFlow("TestFlow", folder);
        Setting[] settingData = createDelimitedFormatterSettings();
        Component component = TestUtils.createComponent(DelimitedFormatter.TYPE, false, null,
                createInputModel(), null, null, createAttributeSettings(), settingData);

        FlowStep formatterFlowStep = new FlowStep();
        formatterFlowStep.setFlowId(flow.getId());
        formatterFlowStep.setComponentId(component.getId());
        formatterFlowStep.setCreateBy("Test");
        formatterFlowStep.setCreateTime(new Date());
        formatterFlowStep.setLastModifyBy("Test");
        formatterFlowStep.setLastModifyTime(new Date());
        formatterFlowStep.setComponent(component);
        return formatterFlowStep;
    }
    
    private static Setting[] createDelimitedFormatterSettings() {

        Setting[] settingData = new Setting[2];
        settingData[0] = new Setting(DelimitedFormatter.DELIMITED_FORMATTER_DELIMITER,"|");
        settingData[1] = new Setting(DelimitedFormatter.DELIMITED_FORMATTER_QUOTE_CHARACTER,"");

        return settingData;
    }
    
    private static List<ComponentAttributeSetting> createAttributeSettings() { 
        
        List<ComponentAttributeSetting> attributeSettings = new ArrayList<ComponentAttributeSetting>();
        attributeSettings.add(new ComponentAttributeSetting("tt1col2", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "10"));
        attributeSettings.add(new ComponentAttributeSetting("tt1col1", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "20"));
        attributeSettings.add(new ComponentAttributeSetting("tt2col2", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "30"));
        attributeSettings.add(new ComponentAttributeSetting("tt2col3", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "50"));
        attributeSettings.add(new ComponentAttributeSetting("tt1col3", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "100"));
        attributeSettings.add(new ComponentAttributeSetting("tt2col1", DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL, "120"));
                
        return attributeSettings;
        
    }
    
    private static Model createInputModel() {

        ModelEntity tt1 = new ModelEntity("tt1", "TEST_TABLE_1");
        tt1.addModelAttribute(new ModelAttribute("tt1col1", tt1.getId(), "COL1"));
        tt1.addModelAttribute(new ModelAttribute("tt1col2", tt1.getId(), "COL2"));
        tt1.addModelAttribute(new ModelAttribute("tt1col3", tt1.getId(), "COL3"));

        ModelEntity tt2 = new ModelEntity("tt2", "TEST_TABLE_2");
        tt2.addModelAttribute(new ModelAttribute("tt2colx", tt1.getId(), "COLX"));
        tt2.addModelAttribute(new ModelAttribute("tt2coly", tt1.getId(), "COLY"));
        tt2.addModelAttribute(new ModelAttribute("tt2colz", tt1.getId(), "COLZ"));

        Model modelVersion = new Model();
        modelVersion.getModelEntities().add(tt1);
        modelVersion.getModelEntities().add(tt2);

        return modelVersion;
    }

    class MessageTarget implements IMessageTarget {

        List<Message> targetMsgArray = new ArrayList<Message>();

        @Override
        public void put(Message message) {
            targetMsgArray.add(message);
        }

        public Message getMessage(int idx) {
            return targetMsgArray.get(idx);
        }

        public int getTargetMessageCount() {
            return targetMsgArray.size();
        }
    }
}
