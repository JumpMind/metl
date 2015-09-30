package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class JoinerTest extends AbstractComponentRuntimeTest {

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
		//((Joiner) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_INPUT_MESSAGE;
		assertEquals("Unit of work not implemented for joiner", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkFlow() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		//((Joiner) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		unitOfWorkLastMessage = true;
		assertEquals("Unit of work not implemented for joiner", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		
		inputMessage.setPayload(PayloadTestHelper.createPayloadWithMultipleEntityData());
		
		List<String> attributesToJoinOn = new ArrayList<String>();
		attributesToJoinOn.add(MODEL_ATTR_ID_1);
		attributesToJoinOn.add(MODEL_ATTR_ID_2);
		((Joiner) spy).attributesToJoinOn = attributesToJoinOn;
		
		runHandle();
		assertHandle(0, 1, 0, 1);
		
		Map<Object, EntityData> joinedData = ((Joiner) spy).joinedData;
		
		assertEquals(1, joinedData.size());
		String expectedKey = MODEL_ATTR_ID_1 + "=" + MODEL_ATTR_NAME_1 + "&" + MODEL_ATTR_ID_2 + "=" + MODEL_ATTR_NAME_2;
		assertTrue(joinedData.containsKey(expectedKey));
		EntityData expectedEntity = joinedData.get(expectedKey);
		assertEquals(3, expectedEntity.size());
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Joiner());
	}
	
	
}
