package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.jumpmind.metl.core.runtime.resource.Http;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

@RunWith(PowerMockRunner.class)
public class WebTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setupHandle();
		setInputMessage(new ControlMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<String>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Override
	public void testHandleNormal() {
		// TODO Auto-generated method stub
		
	}

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try { 
			((Web) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
				.withSetting(Web.RELATIVE_PATH,  "/webhome")
				.withSetting(Web.BODY_FROM, "bodyFrom")
				.withSetting(Web.BODY_TEXT,  "bodyText")
				.withSetting(Web.PARAMETER_REPLACEMENT, "true").build());
		
		Http http = mock(Http.class);
		doReturn(http).when((Web) spy).getResourceRuntime();
		
		((Web) spy).start();
		
		Assert.assertEquals("/webhome", ((Web) spy).relativePath);
		Assert.assertEquals("bodyFrom", ((Web) spy).bodyFrom);
		Assert.assertEquals("bodyText", ((Web) spy).bodyText);
		Assert.assertEquals(true, ((Web) spy).parameterReplacement);
	}

	@Override
	protected String getComponentId() {
		return Web.TYPE;
	}

}
