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

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextConstantTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {

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
		setupHandle();
		setInputMessage(new ControlMessage());
		((TextConstant) spy).constantText = "";
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow(new String("")).buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		((TextConstant) spy).constantText = "";
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow(new String("")).buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO BUCKS";
		((TextConstant) spy).splitOnLineFeed = false;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State").buildString()).build();
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow("GO BUCKS").buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}
	
	@Test
	public void testHandleSplitOnLineFeedNoLineFeed() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO BUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 2;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State\nBuckeyes").buildString()).build();
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow("GO BUCKS").buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}
	
	@Test
	public void testHandleSplitOnLineFeedWithLineFeed() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO\nBUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 2;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State\nBuckeyes").buildString()).build();
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
					.addRow("GO")
					.addRow("BUCKS").buildString()).build();
				
		runHandle();
		assertHandle(2, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}
	
	@Test
	public void testHandleSplitOnLineFeedWithLineFeedAndRowsExceeded() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO\nBUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 1;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State\nBuckeyes").buildString()).build();
		
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
					.addRow("GO")
					.buildString()).build();
				
		Message expectedMessage2 = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
					.addRow("BUCKS").buildString()).build();
		
		runHandle();
		assertHandle(2, getExpectedMessageMonitor(0, 0, false, expectedMessage1, expectedMessage2));
	}

	@Override
	protected String getComponentId() {
		return TextConstant.TYPE;
	}
	
	

}
