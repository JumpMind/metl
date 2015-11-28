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

import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextConstantTest extends AbstractComponentRuntimeTestSupport {

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		
		((TextConstant) spy).start();
		
		assertEquals(1000, ((TextConstant) spy).textRowsPerMessage);
		assertEquals(false, ((TextConstant) spy).splitOnLineFeed);
		assertEquals("", ((TextConstant) spy).constantText);
	}
	
	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
			.withSetting(TextConstant.ROWS_PER_MESSAGE, "10")
			.withSetting(TextConstant.SETTING_SPLIT_ON_LINE_FEED, "true")
			.withSetting(TextConstant.SETTING_TEXT, "replace").build());
		
		((TextConstant) spy).start();
		
		assertEquals(10, ((TextConstant) spy).textRowsPerMessage);
		assertEquals(true, ((TextConstant) spy).splitOnLineFeed);
		assertEquals("replace", ((TextConstant) spy).constantText);
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
		((TextConstant) spy).setRunWhen(Execute.PER_UNIT_OF_WORK);
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		runHandle();
		assertHandle(1);
	}

	@Test
	@Override
	public void testHandleNormal() throws Exception {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO BUCKS";
		((TextConstant) spy).splitOnLineFeed = false;
		
		MessageTestHelper.addInputMessage(this, true, "step1", "Ohio State");
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, "GO BUCKS");
				
		runHandle();
		assertHandle(1);
	}
	
	@Test
	public void testHandleSplitOnLineFeedNoLineFeed() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO BUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 2;
		
		MessageTestHelper.addInputMessage(this, true, "step1", "Ohio State\nBuckeyes");
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, "GO BUCKS");
				
		runHandle();
		assertHandle(1);
	}
	
	@Test
	public void testHandleSplitOnLineFeedWithLineFeed() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO\nBUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 2;
		
		MessageTestHelper.addInputMessage(this, true, "step1", "Ohio State\nBuckeyes");
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, new TextMessage("test").addString("GO").addString("BUCKS"));
				
		runHandle();
		assertHandle(2);
	}
	
	@Test
	public void testHandleSplitOnLineFeedWithLineFeedAndRowsExceeded() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO\nBUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 1;
		
		MessageTestHelper.addInputMessage(this, true, "step1", "Ohio State\nBuckeyes");
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, 
		        new TextMessage("test").addString("GO"),
		        new TextMessage("test").addString("BUCKS"));
		
		runHandle();
		assertHandle(2);
	}

	@Override
	protected String getComponentId() {
		return TextConstant.TYPE;
	}
	
	@Override
	public void setupHandle() {
	    super.setupHandle();
	    ((TextConstant) spy).setRunWhen(Execute.PER_MESSAGE);
	}
	

}
