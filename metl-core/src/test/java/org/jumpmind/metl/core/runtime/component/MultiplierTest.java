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

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class MultiplierTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

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
		((Multiplier) spy).sourceStepId = "test";
		((Multiplier) spy).multipliers = new ArrayList<EntityData>();
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		runHandle();
		assertHandle(0);
	}

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());  
		try {
			((Multiplier) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}
	
	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
				.withSetting(Multiplier.ROWS_PER_MESSAGE, "5")
				.withSetting(Multiplier.MULTIPLIER_SOURCE_STEP, "sourceStep").build());
		
		((Multiplier) spy).start();
		
		Assert.assertEquals(5, ((Multiplier) spy).rowsPerMessage);
		Assert.assertEquals("sourceStep", ((Multiplier) spy).sourceStepId);
	}
	
	@Test
	@Override
	public void testHandleNormal() {
		setupHandle();
		((Multiplier) spy).sourceStepId = "step1";
		
		MessageTestHelper.addInputMessage(this, true, false, "step1", ENTITY_1_KEY_1, ENTITY_1_VALUE_1);
		MessageTestHelper.addInputMessage(this, true, false, "step1", ENTITY_1_KEY_2, ENTITY_1_VALUE_2);
		
		MessageTestHelper.addInputMessage(this, false, false, "step2", ENTITY_2_KEY_1, ENTITY_2_VALUE_1);
		
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(
				this, 
				new MessageBuilder()
					.withKeyValue(ENTITY_2_KEY_1, ENTITY_2_VALUE_1)
					.withKeyValue(ENTITY_1_KEY_1, ENTITY_1_VALUE_1).build(),
				new MessageBuilder()
					.withKeyValue(ENTITY_2_KEY_1, ENTITY_2_VALUE_1)
					.withKeyValue(ENTITY_1_KEY_2, ENTITY_1_VALUE_2).build());
		
		// Execute and Assert
		runHandle();
		assertHandle(2);
	}
	
	@Test
	public void testHandleSourceProvidesNoMessages() {
		setupHandle();
		((Multiplier) spy).sourceStepId = "step1";
		
		MessageTestHelper.addInputMessage(this, false, false, "step2", ENTITY_2_KEY_1, ENTITY_2_VALUE_1);
		
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		
		// Execute and Assert
		runHandle();
		assertHandle(0);
	}
	
	@Test
	public void testHandleMultiplierProvidesNoMessages() {
		setupHandle();
		((Multiplier) spy).sourceStepId = "step1";
		
		MessageTestHelper.addInputMessage(this, true, false, "step1", ENTITY_1_KEY_1, ENTITY_1_VALUE_1);
		
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		
		// Execute and Assert
		runHandle();
		assertHandle(0);
	}


	@Override
	protected String getComponentId() {
		return Multiplier.TYPE;
	}


}
