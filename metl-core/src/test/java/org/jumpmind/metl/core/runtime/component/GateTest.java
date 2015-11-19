package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class GateTest extends AbstractComponentRuntimeTestSupport{

	@Test
	@Override
	public void testHandleStartupMessage() {
		MessageTestHelper.addControlMessage(this, "test", false);
		runHandle();
		assertHandle(0);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		((Gate) spy).gateControlSourceStepId = "step1";
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, 0,1);
		runHandle();
		assertHandle(0);
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		((Gate) spy).gateControlSourceStepId = "step1";
		
		MessageTestHelper.addInputMessage(this, false, "step1", 
				MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		MessageTestHelper.addInputMessage(this, false, "step2", 
				new EntityDataBuilder()
					.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
					.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
					.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
				.build());

		MessageTestHelper.addControlMessage(this, "step2", true);
		MessageTestHelper.addControlMessage(this, "step1", true);

		// Expected
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, 
				new MessageBuilder().withPayload(
						PayloadTestHelper.createPayload(1, 
						MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1, 
						MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2, 
						MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)).build());
				
		// Execute and Assert
		runHandle();
		assertHandle(1);
	}

	@Test
	public void testHandleMissingSecondStep() {
		setupHandle();
		((Gate) spy).gateControlSourceStepId = "step1";
		
		MessageTestHelper.addInputMessage(this, true, "step1", 
				MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
				
		// Execute and Assert
		runHandle();
		assertHandle(0);
	}	

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try {
			((Gate) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder().build());
		
		properties.put(Gate.SOURCE_STEP, "source1");
			
		((Gate) spy).start();
		
		Assert.assertEquals(false, ((Gate) spy).gateOpened);
		Assert.assertEquals("source1", ((Gate) spy).gateControlSourceStepId);
	}

	@Override
	protected String getComponentId() {
		return Gate.TYPE;
	}

}
