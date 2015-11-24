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

import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.ModelAttributeBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class SorterTest extends AbstractComponentRuntimeTestSupport {

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try {
			((Sorter) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	public void testStartIncorrectEntityAttribute() {
		properties.put(Sorter.SORT_ATTRIBUTE, MODEL_ATTR_ID_1);
		
		setupStart(new SettingsBuilder().build());
		try {
			((Sorter) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}
	
	@Test
	public void testStartAttributeMissingFromModel() {
		setupStart(new SettingsBuilder().build());
				
		properties.put(Sorter.SORT_ATTRIBUTE, "ENTITY1." + MODEL_ATTR_ID_1);
		
		Mockito.when(inputModel.getAttributeByName("ENTITY1", MODEL_ATTR_ID_1))
			.thenReturn(new ModelAttributeBuilder().withId(MODEL_ATTR_ID_1).build());
		
		((Sorter) spy).start();
		Assert.assertEquals(MODEL_ATTR_ID_1, ((Sorter) spy).sortAttributeId);
	}

	
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
			.withSetting(Sorter.SORT_ATTRIBUTE, MODEL_ATTR_ID_1)
			.withSetting(Sorter.ROWS_PER_MESSAGE, "5").build());
		((Sorter) spy).start();
		
		
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
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		runHandle();
		assertHandle(0);
	}

	@Test
	@Override
	public void testHandleNormal() throws Exception {
		// Setup
		setupHandle();
		((Sorter) spy).sortAttributeId = MODEL_ATTR_ID_1;
		
		MessageTestHelper.addInputMessage(this, false, "step1", MODEL_ATTR_ID_1, "superman");
		MessageTestHelper.addInputMessage(this, false, "step2", MODEL_ATTR_ID_1, "iron man");
		MessageTestHelper.addInputMessage(this, true, "step2", MODEL_ATTR_ID_1, "flash");
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		MessageTestHelper.addOutputMonitor(this, 
				MessageTestHelper.nullMessage(),
				new MessageBuilder().withKeyValue(MODEL_ATTR_ID_1, "flash").build(),
				new MessageBuilder().withKeyValue(MODEL_ATTR_ID_1, "iron man").build(),
				new MessageBuilder().withKeyValue(MODEL_ATTR_ID_1, "superman").build());
		
		runHandle();
		assertHandle(3);
		
	}

	@Override
	protected String getComponentId() {
		return Sorter.TYPE;
	}

}
