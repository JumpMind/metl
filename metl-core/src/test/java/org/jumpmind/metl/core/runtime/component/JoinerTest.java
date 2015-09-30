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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class JoinerTest extends AbstractComponentRuntimeTest<ArrayList<EntityData>> {

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
		assertEquals("Unit of work not implemented for joiner", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkFlow() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		assertEquals("Unit of work not implemented for joiner", 1,2);
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		
		inputMessage.setPayload(PayloadTestHelper.createPayloadWithMultipleEntityData());
		
		List<String> attributesToJoinOn = new ArrayList<String>();
		attributesToJoinOn.add(MODEL_ATTR_ID_1);
		attributesToJoinOn.add(MODEL_ATTR_ID_2);
		((Joiner) spy).attributesToJoinOn = attributesToJoinOn;
		
		runHandle();
		assertHandle(0, 1, 0, 1);
		
		Map<Object, EntityData> joinedData = ((Joiner) spy).joinedData;
		
		assertEquals(1, joinedData.size());
		String expectedKey = MODEL_ATTR_ID_1 + "=" + MODEL_ATTR_NAME_1 + "&" + MODEL_ATTR_ID_2 + "=" + MODEL_ATTR_NAME_2;
		assertTrue(joinedData.containsKey(expectedKey));
		EntityData expectedEntity = joinedData.get(expectedKey);
		assertEquals(3, expectedEntity.size());
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new Joiner());
	}
	
	
}
