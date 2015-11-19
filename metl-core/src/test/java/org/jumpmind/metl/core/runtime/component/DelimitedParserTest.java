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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.DelimitedParser.AttributeFormat;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.ModelHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class DelimitedParserTest extends AbstractComponentRuntimeTestSupport {

	@Override
	public void testStartDefaults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testStartWithValues() {
		// TODO Auto-generated method stub
		
	}
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new ControlMessage());

		// Expected
		Message expectedMessage = new MessageBuilder().withPayload(
				new PayloadBuilder().buildED()).build();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
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
	public void testHandleNormal() {
		setupHandle();
		((DelimitedParser) spy).attributes = new ArrayList<AttributeFormat>();
		
		AttributeFormat mAttr = ((DelimitedParser) spy).new AttributeFormat(
				ModelHelper.ATTR_ID + "0_0", null, null);
		mAttr.setOrdinal(1);
		
		((DelimitedParser) spy).attributes.add(mAttr);
		
		Model model = ModelHelper.createSimpleModel(2, 3);
		this.outputModel = model;
		when(component.getOutputModel()).thenReturn(model);
		
		MessageTestHelper.addInputMessage(this, true, "step1", "red,yellow,green,blue,purple,orange");
		MessageTestHelper.addOutputMonitor(this, ModelHelper.ATTR_ID + "0_0", "red");
		
		runHandle();
		assertHandle(1);
	}
	
	@Test
	public void testHandleWithScriptEval() {
		// Setup
		setupHandle();
		
		Model model = ModelHelper.createSimpleModel(2, 3);
		when(component.getOutputModel()).thenReturn(model);
		
		((DelimitedParser) spy).attributes = new ArrayList<AttributeFormat>();
		AttributeFormat mAttr = ((DelimitedParser) spy).new AttributeFormat(
				ModelHelper.ATTR_ID + "0_0", null, null);
		mAttr.setOrdinal(1);
		mAttr.setFormatFunction("someFunction");
		((DelimitedParser) spy).attributes.add(mAttr);
		
		
		PowerMockito.mockStatic(ModelAttributeScriptHelper.class);
		try {
			PowerMockito.doReturn("maroon").when(
				ModelAttributeScriptHelper.class, 
				"eval",
                Mockito.any(Message.class), 
                Mockito.any(ComponentContext.class), 				
				Mockito.any(ModelAttribute.class), 
				Mockito.anyObject(), 
				Mockito.any(ModelEntity.class), 
				Mockito.any(EntityData.class), 
				Mockito.anyString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		MessageTestHelper.addInputMessage(this, true, "step1", "red,yellow,green,blue,purple,orange");
		MessageTestHelper.addOutputMonitor(this, ModelHelper.ATTR_ID + "0_0", "maroon");
		
		runHandle();
		assertHandle(1);
	}
	
	@Test
	public void testHandleWithoutAttributesSet() {
		// Setup
		setupHandle();
		Model model = ModelHelper.createSimpleModel(2, 3);
		
		this.outputModel = model;
		when(component.getOutputModel()).thenReturn(model);
		
		MessageTestHelper.addInputMessage(this, true, "step1", "red,yellow,green,blue,purple,orange");
		MessageTestHelper.addOutputMonitor(this, new MessageBuilder().withPayload(
				new PayloadBuilder()
				.withRow(new EntityDataBuilder()
					.withKV(ModelHelper.ATTR_ID + "0-0", "red")
					.withKV(ModelHelper.ATTR_ID + "1-0", "yellow")
					.withKV(ModelHelper.ATTR_ID + "2-0", "green")
					.withKV(ModelHelper.ATTR_ID + "0-1", "blue")
					.withKV(ModelHelper.ATTR_ID + "1-1", "purple")
					.withKV(ModelHelper.ATTR_ID + "2-1", "orange")
				.build()).buildED()).build());
		
		runHandle();
		assertHandle(1);
	}
	
	@Test
	public void testHandleSkipHeaderAndFooterRows() {
		setupHandle();
		((DelimitedParser) spy).numberOfHeaderLinesToSkip = 1;
		((DelimitedParser) spy).numberOfFooterLinesToSkip = 1;
		Model model = ModelHelper.createSimpleModel(2, 3);
		when(component.getOutputModel()).thenReturn(model);
		
		MessageTestHelper.addInputMessage(this, true, "step1", "h1,h2,h3,h4,h5,h6",
				"red,yellow,green,blue,purple,orange",
				"f1,f2,f3,f4,f5,f6");
		
		MessageTestHelper.addOutputMonitor(this, new MessageBuilder().withPayload(
				new PayloadBuilder()
				.withRow(new EntityDataBuilder()
					.withKV(ModelHelper.ATTR_ID + "0-0", "red")
					.withKV(ModelHelper.ATTR_ID + "1-0", "yellow")
					.withKV(ModelHelper.ATTR_ID + "2-0", "green")
					.withKV(ModelHelper.ATTR_ID + "0-1", "blue")
					.withKV(ModelHelper.ATTR_ID + "1-1", "purple")
					.withKV(ModelHelper.ATTR_ID + "2-1", "orange")
				.build()).buildED()).build());
		
		runHandle();
		assertHandle(1);
	}


	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new DelimitedParser());
	}

	@Override
	protected String getComponentId() {
		return DelimitedParser.TYPE;
	}

}
