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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.MessageAssert;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadTestHelper;
import org.jumpmind.metl.core.utils.TestUtils;
import org.jumpmind.properties.TypedProperties;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;

public abstract class AbstractComponentRuntimeTestSupport {

	public static String MODEL_ATTR_ID_1 = "attr1";
	public static String MODEL_ATTR_NAME_1 = "attr1Name";
	public static String MODEL_ATTR_ID_2 = "attr2";
	public static String MODEL_ATTR_NAME_2 = "attr2Name";
	public static String MODEL_ATTR_ID_3 = "attr3";
	public static String MODEL_ATTR_NAME_3 = "attr3Name";
	public static String MODEL_ATTR_ID_4 = "attr4";
	public static String MODEL_ATTR_NAME_4 = "attr4Name";
	
	public static String ENTITY_1_KEY_1 = "e1.col1";
	public static String ENTITY_1_VALUE_1 = "val1";
	public static String ENTITY_1_KEY_2 = "e1.col2";
	public static String ENTITY_1_VALUE_2 = "val2";
	public static String ENTITY_2_KEY_1 = "e2.col1";
	public static String ENTITY_2_VALUE_1 = "val3";
	
	private ComponentRuntimeFromXMLFactory componentRuntimeFromXMLFactory;
	
	// Standard tests that should be implemented for all components
	public abstract void testHandleStartupMessage();
	public abstract void testHandleUnitOfWorkLastMessage();
	public abstract void testHandleNormal() throws Exception;
	public abstract void testStartDefaults();
	public abstract void testStartWithValues();
	
	abstract protected String getComponentId();
	
	protected ComponentRuntimeFromXMLFactory getComponentRuntimeFromXMLFactory() {
	    if (componentRuntimeFromXMLFactory == null) {
	        componentRuntimeFromXMLFactory = new ComponentRuntimeFromXMLFactory();
	    }
	    return componentRuntimeFromXMLFactory;
	}
	
	public IComponentRuntime getComponentSpy() {
	    return Mockito.spy(getComponentRuntimeFromXMLFactory().create(getComponentId()));
	}
	
	IComponentRuntime spy;
	
	List<HandleParams> messages = new ArrayList<HandleParams>();
	List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
	


	Model inputModel;
	Model outputModel;
	Component component;
	ComponentContext context;
	ComponentStatistics componentStatistics;
	Map<String, String> flowParameters;
	FlowStep flowStep;
	Flow flow;
	ExecutionTrackerNoOp eExecutionTracker;
	TypedProperties properties;
	
	// internal testing variable so setupHandle and runHandle can be called independently
	boolean setupCalled;
	
	@Before
	public void reset() {
		spy = getComponentSpy();		
		
		initHandleParams();
		
		setupCalled = false;
		context = mock(ComponentContext.class);
		inputModel = mock(Model.class);
		outputModel = mock(Model.class);
		component = mock(Component.class);
		
		componentStatistics = new ComponentStatistics();
		flowParameters = new HashMap<>();
		flowStep = mock(FlowStep.class);
		flow = mock(Flow.class);
		
		eExecutionTracker = new ExecutionTrackerNoOp();
		properties = new TypedProperties();
	}	
	
	public void setupStart(Component component) {
		when(context.getFlowStep()).thenReturn(flowStep);
		when(context.getManipulatedFlow()).thenReturn(flow);
		when(flowStep.getComponent()).thenReturn(component);
		when(flow.findFlowStepWithId(Mockito.anyString())).thenReturn(flowStep);
		when(flow.findLinkBetweenSourceAndTarget(Mockito.anyString(), Mockito.anyString())).thenReturn(new FlowStepLink());
		
		component.setInputModel(inputModel);
		component.setOutputModel(outputModel);
		
	    doReturn(properties).when((AbstractComponentRuntime) spy).getTypedProperties();
	    doReturn(flowStep).when((AbstractComponentRuntime) spy).getFlowStep();
	    
	    doReturn(component).when((AbstractComponentRuntime) spy).getComponent();
	    ((AbstractComponentRuntime) spy).setContext(context);
	}
	
	public void setupStart(List<Setting> settings) {
		Component component = new Component();
		component.setSettings(settings);
		setupStart(component);
	}

	public void setupHandle() {
		messages.clear();
		expectedMonitors.clear();
		
		doNothing().when((AbstractComponentRuntime) spy).start();
		
		componentStatistics = new ComponentStatistics();
		ExecutionTrackerNoOp eExecutionTracker = new ExecutionTrackerNoOp();
		
		when(context.getComponentStatistics()).thenReturn(componentStatistics);
		when(context.getFlowParameters()).thenReturn(flowParameters);
		when(context.getFlowStep()).thenReturn(flowStep);
		
		when(component.getInputModel()).thenReturn(inputModel);
		when(component.getOutputModel()).thenReturn(outputModel);
		
		when(flowStep.getComponent()).thenReturn(component);
		
		doReturn(eExecutionTracker).when((AbstractComponentRuntime) spy).getExecutionTracker();
		doReturn(new TypedProperties()).when((AbstractComponentRuntime) spy).getTypedProperties();
		
		spy.start(0, context);
		setupCalled = true;
	}
	
	public void runHandle() {
		if (!setupCalled) {
			setupHandle();
		}
		
		for(int i = 0; i < messages.size(); i++) {
			HandleParams p = messages.get(i);
			spy.handle(p.getInputMessage(), p.getCallback(), p.getUnitOfWorkLastMessage());
		}
	}
	
	public HandleMessageMonitor getExpectedMessageMonitor(int starts, int shutdowns) {
		Message m = null;
		return getExpectedMessageMonitor(starts, shutdowns, false, m);
	}
	
	public HandleMessageMonitor getExpectedMessageMonitor(boolean xmlPayload, Message... messages) {
		return getExpectedMessageMonitor(0, 0, xmlPayload, messages);
	}
	
	public HandleMessageMonitor getExpectedMessageMonitor(Message... messages) {
		return getExpectedMessageMonitor(0, 0, false, messages);
	}
		
	public HandleMessageMonitor getExpectedMessageMonitor(int starts, int shutdowns, boolean xmlPayload, Message... messages) {
		HandleMessageMonitor m = new HandleMessageMonitor();
		m.setStartupMessageCount(starts);
		m.setShutdownMessageCount(shutdowns);
		m.setXmlPayload(xmlPayload);
		if (messages == null || messages[0] == null) {
			m.setMessages(new ArrayList<Message>());
		}
		else {
			m.setMessages(Arrays.asList(messages));
		}
		return m;
	}
	
	public void assertHandle(int numberEntitiesProcessed) {
		
		TestUtils.assertNullNotNull(expectedMonitors, messages);
		
		if (messages != null) {
			Assert.assertEquals("Expected monitors size should match the number of input messages.", 
					expectedMonitors.size(), messages.size());
			
			// Loop through all input messages 
			for (int i = 0; i < messages.size(); i++) {
				HandleMessageMonitor expected = expectedMonitors.get(i);
				HandleMessageMonitor actual = messages.get(i).getCallback().getMonitor();
			
				assertEquals("The number of entities processed are not equal.  ", numberEntitiesProcessed, 
						((AbstractComponentRuntime) spy).getComponentStatistics().getNumberEntitiesProcessed(0));
				//assertEquals("Send message counts do not match [message " + (i + 1) + "]", expected.getSendMessageCount(), actual.getSendMessageCount());
				//assertEquals("Start message counts do not match [message " + (i + 1) + "]", expected.getStartupMessageCount(), actual.getStartupMessageCount());
				//assertEquals("Shutdown message counts do not match [message " + (i + 1) + "]", expected.getShutdownMessageCount(), actual.getShutdownMessageCount());
				TestUtils.assertList(expected.getTargetStepIds(), actual.getTargetStepIds(), expected.isXmlPayload());
			
				for (int m = 0; m < expected.getMessages().size(); m++) {
					MessageAssert.assertMessage(i + 1, m + 1, expected.getMessages().get(m), actual.getMessages().size() > m ? actual.getMessages().get(m) : null, expected.isXmlPayload());
				}
			}
		}
	}
	
	public void setInputMessage(Message inputMessage) {
		messages.get(0).setInputMessage(inputMessage);
	}
	public void setUnitOfWorkLastMessage(Boolean uof) {
		messages.get(0).setUnitOfWorkLastMessage(uof);
	}
	
	public Message getInputMessage() {
		return messages.get(0).getInputMessage();
	}
	
	public void initHandleParams() {
		messages = new ArrayList<HandleParams>();
		EntityDataMessage m = new EntityDataMessage("step1");
		m.setPayload(PayloadTestHelper.createPayloadWithMultipleEntityData());
		HandleParams params = new HandleParams(m);
		messages.add(params);
	}
	
	public List<HandleParams> getMessages() {
		return messages;
	}
	
	public List<HandleMessageMonitor> getExpectedMonitors() {
		return expectedMonitors;
	}
	
}
