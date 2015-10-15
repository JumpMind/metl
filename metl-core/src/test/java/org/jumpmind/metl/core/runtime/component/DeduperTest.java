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
import java.util.LinkedHashMap;
import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DeduperTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

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
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		Message message1 = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).build())
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).build())
					.buildED()).build();
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		((Deduper) spy).deduped = new LinkedHashMap<String, EntityData>();
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayload(new PayloadBuilder()
				.addRow(new EntityDataBuilder()
					.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
				.build()).buildED()).build();

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
		
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}

	@Override
	protected String getComponentId() {
	    return Deduper.TYPE;
	}

	
}
