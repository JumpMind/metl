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

import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class JoinerTest extends AbstractComponentRuntimeTest<ArrayList<EntityData>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(0, 0, 0, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(0, 0, 0, 0));
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
				.setPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
						.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
				.build()).buildED()).build();
		
		Message message2 = new MessageBuilder("step1")
				.setPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
						.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
				.build()).buildED()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, false));
		messages.add(new HandleParams(message2, true));
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
							.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
							.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
						.build()).buildED();
						

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, 0, 0));
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
				
		// Execute and Assert
		runHandle();
		assertHandle(2, expectedMonitors);
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Joiner());
	}
	
	
}
