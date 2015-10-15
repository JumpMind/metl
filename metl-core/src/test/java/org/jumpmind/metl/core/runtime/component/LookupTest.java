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
import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class LookupTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new StartupMessage());
		((Lookup) spy).sourceStepId = "step1";
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		((Lookup) spy).sourceStepId = "step1";
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Lookup setup
		setupHandle();
		
		((Lookup) spy).sourceStepId = "step1";
		((Lookup) spy).keyAttributeId = MODEL_ATTR_ID_1;
		((Lookup) spy).valueAttributeId = MODEL_ATTR_ID_2;
		((Lookup) spy).replacementKeyAttributeId = MODEL_ATTR_ID_1;
		((Lookup) spy).replacementValueAttributeId = MODEL_ATTR_ID_3;
		
		// Messages
		Message message1 = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
					.build()).buildED()).build();
			
		Message message2 = new MessageBuilder("step2")
				.withPayload(new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
							.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
							.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
					.build()).buildED()).build();
		
		Message message3 = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
							.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
							.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
					.build()).buildED()).build();
		message3.getHeader().setUnitOfWorkLastMessage(true);
		
		messages.clear();
		messages.add(new HandleParams(message1, false));
		messages.add(new HandleParams(message2, false));
		messages.add(new HandleParams(message3, true));
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayload(
				PayloadTestHelper.createPayload(1, 
				MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1, 
				MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2, 
				MODEL_ATTR_ID_3, MODEL_ATTR_NAME_2)).build();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(0, 0));
		expectedMonitors.add(getExpectedMessageMonitor(0, 0));
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, expectedMessage));
		
			
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
    @Override
    protected String getComponentId() {
        return Lookup.TYPE;
    }

	
}
