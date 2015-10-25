/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.jumpmind.metl.core.runtime.resource.Http;
import org.jumpmind.metl.core.runtime.resource.HttpOutputStream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class WebTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		((Web) spy).bodyFrom = "Message";
		
		setInputMessage(new ControlMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		((Web) spy).bodyFrom = "Message";
		
		
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
	
	@Override
	public void setupHandle() {
		super.setupHandle();
		
		HttpOutputStream outputStream = mock(HttpOutputStream.class);
		Mockito.when(resource.getOutputStream(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(outputStream);
		
		
	}

}
