package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class LookupTest extends AbstractComponentRuntimeTest<ArrayList<EntityData>> {

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
		//((Lookup) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_INPUT_MESSAGE;
		assertEquals("Unit of work not implemented for lookup", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkFlow() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		//((Lookup) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		unitOfWorkLastMessage = true;
		assertEquals("Unit of work not implemented for lookup", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		
		inputMessage.setPayload(PayloadTestHelper.createPayloadWithMultipleEntityData());
		
		inputMessage.getHeader().setUnitOfWorkLastMessage(false);
		//((Lookup) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		((Lookup) spy).keyAttributeId = MODEL_ATTR_ID_1;
		((Lookup) spy).valueAttributeId = MODEL_ATTR_ID_2;
		
		runHandle();
		assertHandle(1, 1, 1, 1, false);
		assertFalse(((Lookup) spy).lookupInitialized);
		assertEquals(1, ((Lookup) spy).lookup.size());
		assertTrue(((Lookup) spy).lookup.containsKey(MODEL_ATTR_ID_1));
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Lookup());
	}

	public void setupHandle() {
		super.setupHandle();
		((Lookup) spy).sourceStepId = "test";
		inputMessage.getHeader().setOriginatingStepId("test");
	}
}
