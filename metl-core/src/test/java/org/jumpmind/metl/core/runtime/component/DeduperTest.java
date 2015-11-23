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

import java.util.LinkedHashMap;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;



@RunWith(PowerMockRunner.class)
public class DeduperTest extends AbstractComponentRuntimeTestSupport {

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		((Deduper) spy).start();
		
		Assert.assertEquals(10000, ((Deduper) spy).rowsPerMessage);
	}

	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
				.withSetting(Deduper.ROWS_PER_MESSAGE, "5").build());
		
		((Deduper) spy).start();
		
		Assert.assertEquals(5, ((Deduper) spy).rowsPerMessage);
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
		setupHandle();
		((Deduper) spy).deduped = new LinkedHashMap<String, EntityData>();
		
		MessageTestHelper.addInputMessage(this, true, "step1", new EntityDataBuilder().withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).build(),
				new EntityDataBuilder().withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).build());
		
		MessageTestHelper.addOutputMonitor(this, MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		// Execute and Assert
		runHandle();
		assertHandle(1);
	}
	
	@Test
	public void testHandleLastMessageHeaderNotUnitOfWork() {
		setupHandle();
		((Deduper) spy).deduped = new LinkedHashMap<String, EntityData>();
		
		// Message header has last message set to true but unit of work passed into handle is false.
		MessageTestHelper.addInputMessage(this, false, "step1", new EntityDataBuilder().withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).build(),
				new EntityDataBuilder().withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).build());
		
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		
		// Execute and Assert
		runHandle();
		assertHandle(1);
	}


	@Override
	protected String getComponentId() {
	    return Deduper.TYPE;
	}



	
}
