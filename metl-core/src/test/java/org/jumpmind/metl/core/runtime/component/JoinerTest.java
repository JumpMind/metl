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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentAttributeSettingsBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class JoinerTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		
		try {
			((Joiner) spy).start();
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}
	}
	
	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder().build());
		
		((Joiner) spy).getComponent().setInputModel(new Model());
		((Joiner) spy).getComponent().setAttributeSettings(
				new ComponentAttributeSettingsBuilder()
				.withSetting(MODEL_ATTR_ID_1, "1", Joiner.JOIN_ATTRIBUTE, "true").build());
		
		((Joiner) spy).start();
		
		List<String> expectedList = new ArrayList<String>();
		expectedList.add(MODEL_ATTR_ID_1);
		
		TestUtils.assertList(expectedList, ((Joiner) spy).attributesToJoinOn, false);
	}
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new ControlMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Join setup
		setupHandle();
		
		List<String> attributesToJoinOn = new ArrayList<String>();
		attributesToJoinOn.add(MODEL_ATTR_ID_1);
		((Joiner) spy).attributesToJoinOn = attributesToJoinOn;
		
		// Messages
		Message message1 = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
						.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
				.build()).buildED()).build();
		
		Message message2 = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
						.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
				.build()).buildED()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, false));
		messages.add(new HandleParams(message2, true));
		
		// Expected
		Message expectedMessage = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
							.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
							.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
						.build()).buildED()).build();
						

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(0, 0));
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, expectedMessage));
				
		// Execute and Assert
		runHandle();
		assertHandle(2, expectedMonitors);
	}

    @Override
    protected String getComponentId() {
        return Joiner.TYPE;
    }
	
	
}
