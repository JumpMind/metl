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
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class LookupTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try {
			((Lookup) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder().build());
		
		properties.put(Lookup.SOURCE_STEP, "source1");
		properties.put(Lookup.LOOKUP_KEY, MODEL_ATTR_ID_1);
		properties.put(Lookup.LOOKUP_VALUE, "value");
		properties.put(Lookup.REPLACEMENT_KEY_ATTRIBUTE, MODEL_ATTR_ID_2);
		properties.put(Lookup.REPLACEMENT_VALUE_ATTRIBUTE, "value2");
			
		((Lookup) spy).start();
		
		Assert.assertEquals(false, ((Lookup) spy).lookupInitialized);
		Assert.assertEquals("source1", ((Lookup) spy).sourceStepId);
		Assert.assertEquals(MODEL_ATTR_ID_1, ((Lookup) spy).keyAttributeId);
		Assert.assertEquals("value", ((Lookup) spy).valueAttributeId);
		Assert.assertEquals(MODEL_ATTR_ID_2, ((Lookup) spy).replacementKeyAttributeId);
		Assert.assertEquals("value2", ((Lookup) spy).replacementValueAttributeId);
	}
	
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
		((Lookup) spy).sourceStepId = "step1";
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, 0,1);
		runHandle();
		assertHandle(0);
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
		
		MessageTestHelper.addInputMessage(this, false, false, "step1", 
				MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		MessageTestHelper.addInputMessage(this, false, false, "step2", 
				new EntityDataBuilder()
					.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
					.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
					.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
				.build());
					
		MessageTestHelper.addInputMessage(this, true, true, "step1", 
				new EntityDataBuilder()
					.withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
					.withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
					.withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3)
				.build());
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, 
				new MessageBuilder().withPayload(
					PayloadTestHelper.createPayload(1, 
					MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1, 
					MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2, 
					MODEL_ATTR_ID_3, MODEL_ATTR_NAME_2)).build());
		
		// Execute and Assert
		runHandle();
		assertHandle(1);
	}
	
    @Override
    protected String getComponentId() {
        return Lookup.TYPE;
    }

	
}
