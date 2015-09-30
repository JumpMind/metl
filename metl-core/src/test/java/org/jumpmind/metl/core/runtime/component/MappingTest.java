package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.ModelTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class MappingTest extends AbstractComponentRuntimeTest {

	public static String MAPPING_TARGET_1 = "mapping1";
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		inputMessage = new StartupMessage();
		runHandle();
		assertHandle(0, 1, 0, 0);
	}

	@Test
	@Override
	public void testHandleShutdownMessage() {
		inputMessage = new ShutdownMessage("test");
		runHandle();
		assertHandle(0, 1, 0, 0);
	}

	@Test 
	@Override
	public void testHandleEmptyPayload() {
		setupHandle();
		runHandle();
		assertHandle(0, 1, 0, 0);
	}
	
	@Test 
	@Override
	public void testHandleUnitOfWorkInputMessage() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		((Mapping) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_INPUT_MESSAGE;
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}
	
	@Test 
	@Override
	public void testHandleUnitOfWorkFlow() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		((Mapping) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		unitOfWorkLastMessage = true;
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}
	
	@Test 
	@Override
	public void testHandleNormal() {
		setupHandle();
		
		inputMessage.setPayload(PayloadTestHelper.createPayloadWithEntityData(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1));
		HashMap<String, Set<String>> attrToAttrMap = new HashMap<String, Set<String>>();
		Set<String> mappings = new HashSet<String>();
		mappings.add(MAPPING_TARGET_1);
		
		attrToAttrMap.put(MODEL_ATTR_ID_1, mappings);
		
		((Mapping) spy).attrToAttrMap = attrToAttrMap;
		((Mapping) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		
		runHandle();
		assertHandle(1, 1, 1, 1, false, MAPPING_TARGET_1, MODEL_ATTR_NAME_1);
	}
	
	@Test 
	public void testHandleUnMappedToNull() {
		setupHandle();
		
		inputMessage.setPayload(PayloadTestHelper.createPayloadWithEntityData(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1));
		
		HashMap<String, Set<String>> attrToAttrMap = new HashMap<String, Set<String>>();
		Set<String> mappings = new HashSet<String>();
		mappings.add(MAPPING_TARGET_1);
		attrToAttrMap.put("X", mappings);
		
		ModelTestHelper.createMockModel(outputModel, MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1, MODEL_ENTITY_ID_1, MODEL_ENTITY_NAME_1);
		
		((Mapping) spy).attrToAttrMap = new HashMap<String, Set<String>>();
		((Mapping) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		((Mapping) spy).setUnmappedAttributesToNull = true;
		
		runHandle();
		assertHandle(1, 1, 1, 1, false, MAPPING_TARGET_1, null);
	}
	
	@Test 
	public void testHandleNoMappingsFound() {
		setupHandle();
		
		inputMessage.setPayload(PayloadTestHelper.createPayloadWithEntityData(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1));
		HashMap<String, Set<String>> attrToAttrMap = new HashMap<String, Set<String>>();
		Set<String> mappings = new HashSet<String>();
		mappings.add(MAPPING_TARGET_1);
		
		attrToAttrMap.put("X", mappings);
		
		((Mapping) spy).attrToAttrMap = attrToAttrMap;
		((Mapping) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		
		runHandle();
		assertHandle(1, 1, 1, 0);
	}
	
	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Mapping());
	}

}
