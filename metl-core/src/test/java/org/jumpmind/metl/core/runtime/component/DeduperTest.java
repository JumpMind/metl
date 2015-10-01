package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DeduperTest extends AbstractComponentRuntimeTest<ArrayList<EntityData>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(1,0, getExpectedMonitorSingle(0, 0, 0, -1, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(1,0, getExpectedMonitorSingle(0, 0, 0, -1, 0));
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		Message message1 = getInputMessage();
		ArrayList<EntityData> payload = PayloadTestHelper.createPayloadWithEntityData(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		PayloadTestHelper.addEntityDataToPayload(payload, MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		message1.setPayload(payload);
		
		((Deduper) spy).deduped = new LinkedHashMap<String, EntityData>();
		
		runHandle();
		assertHandle(1,1, getExpectedMonitorSingle(1, 0, 0, 0, 1));
		assertEquals("Deduped map size incorrect", 0, ((Deduper) spy).deduped.size());
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Deduper());
	}

	
}
