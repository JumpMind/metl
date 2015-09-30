package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DeduperTest extends AbstractComponentRuntimeTest {

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
		//((Deduper) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_INPUT_MESSAGE;
		assertEquals("Unit of work not implemented for deduper", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkFlow() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		//((Deduper) spy).unitOfWork = AbstractComponentRuntime.UNIT_OF_WORK_FLOW;
		unitOfWorkLastMessage = true;
		assertEquals("Unit of work not implemented for deduper", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		
		inputMessage.setPayload(PayloadTestHelper.createPayloadWithEntityData(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1));
		ArrayList<EntityData> payload = inputMessage.getPayload();
		PayloadTestHelper.addEntityDataToPayload(payload, MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		((Deduper) spy).deduped = new LinkedHashMap<String, EntityData>();
		
		runHandle();
		assertHandle(0, 1, 0, 1);
		//assertHandle(1, 1, 1, 1);
		assertEquals("Deduped map size incorrect", 1, ((Deduper) spy).deduped.size());
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Deduper());
	}

	
}
