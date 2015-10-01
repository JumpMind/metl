package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.ModelTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class TransformerTest extends AbstractComponentRuntimeTest<ArrayList<EntityData>> {
	
	public static String TRANSFORM_SOURCE = "transform me";
	public static String TRANSFORM_EXP = "transform logic";
	public static String TRANSFORM_RESULT = "transformed";
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(1, 1, 1, 0);
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
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		//((Transformer) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_INPUT_MESSAGE;
		assertEquals("Unit of work not implemented for transformer", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkFlow() {
		setupHandle();
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		//((Transformer) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		setUnitOfWorkLastMessage(true);
		assertEquals("Unit of work not implemented for transformer", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		
		getInputMessage().setPayload(PayloadTestHelper.createPayloadWithEntityData(MODEL_ATTR_ID_1, TRANSFORM_SOURCE));
		
		Map<String, String> transformMap = new HashMap<String, String>();
		transformMap.put(MODEL_ATTR_ID_1, TRANSFORM_EXP);
		
		((Transformer) spy).transformsByAttributeId = transformMap;
		
		PowerMockito.mockStatic(ModelAttributeScriptHelper.class);
		
		try {
			PowerMockito.doReturn(TRANSFORM_RESULT).when(
				ModelAttributeScriptHelper.class, 
				"eval",
				Mockito.any(ModelAttribute.class), 
				Mockito.anyObject(), 
				Mockito.any(ModelEntity.class), 
				Mockito.any(EntityData.class), 
				Mockito.anyString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		runHandle();
		assertHandle(1, 1, 1, 1, false, MODEL_ATTR_ID_1, TRANSFORM_RESULT);
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Transformer());
	}

	@Override
	public void setupHandle() {
		super.setupHandle();
		
		ArrayList<EntityData> payload = new ArrayList<EntityData>(); 
		getInputMessage().setPayload(payload);
		ModelTestHelper.createMockModel(inputModel, MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1, 
				MODEL_ATTR_ID_1, MODEL_ENTITY_NAME_1);
	}

	
}
