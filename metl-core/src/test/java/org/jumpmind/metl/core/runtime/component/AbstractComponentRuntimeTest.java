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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Before;
import org.junit.rules.ExpectedException;

public abstract class AbstractComponentRuntimeTest<T> {

	public static String MODEL_ATTR_ID_1 = "attr1";
	public static String MODEL_ATTR_NAME_1 = "attr1Name";
	public static String MODEL_ATTR_ID_2 = "attr2";
	public static String MODEL_ATTR_NAME_2 = "attr2Name";
	public static String MODEL_ATTR_ID_3 = "attr3";
	public static String MODEL_ATTR_NAME_3 = "attr3Name";
	
	public static String MODEL_ENTITY_ID_1 = "entity1";
	public static String MODEL_ENTITY_NAME_1 = "entity1Name";
	public static String MODEL_ENTITY_ID_2 = "entity2";
	public static String MODEL_ENTITY_NAME_2 = "entity2Name";
	public static String MODEL_ENTITY_ID_3 = "entity3";
	public static String MODEL_ENTITY_NAME_3 = "entity3Name";
	
	// Standard tests that should be implemented for all components
	public abstract void testHandleStartupMessage();
	public abstract void testHandleUnitOfWorkLastMessage();
	public abstract void testHandleNormal();
	
	public abstract IComponentRuntime getComponentSpy();
	
	IComponentRuntime spy;
	
	List<HandleParams> messages = new ArrayList<HandleParams>();
		
	Model inputModel;
	Model outputModel;
	Component component;
	ComponentContext context;
	ComponentStatistics componentStatistics;
	Map<String, String> flowParametersAsString;
	Map<String, Serializable> flowParameters;
	FlowStep flowStep;
	ExecutionTrackerNoOp eExecutionTracker;
	
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
		flowParametersAsString = new HashMap<String, String>();
		flowParameters = new HashMap<String, Serializable>();
		flowStep = mock(FlowStep.class);
		eExecutionTracker = new ExecutionTrackerNoOp();
	}
	
	public void setupHandle() {
		doNothing().when((AbstractComponentRuntime) spy).start();
		
		componentStatistics = new ComponentStatistics();
		ExecutionTrackerNoOp eExecutionTracker = new ExecutionTrackerNoOp();
		
		when(context.getComponentStatistics()).thenReturn(componentStatistics);
		when(context.getFlowParametersAsString()).thenReturn(flowParametersAsString);
		when(context.getFlowParameters()).thenReturn(flowParameters);
		when(context.getFlowStep()).thenReturn(flowStep);
		
		when(component.getInputModel()).thenReturn(inputModel);
		when(component.getOutputModel()).thenReturn(outputModel);
		
		when(flowStep.getComponent()).thenReturn(component);
		
		doReturn(eExecutionTracker).when((AbstractComponentRuntime) spy).getExecutionTracker();
		
		spy.start(context);
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
	
	public List<HandleMonitor> getExpectedMonitorSingle(int sends, int starts, int shutdowns, int lastIndex, int expectedPayloadSize) {
		List<HandleMonitor> list = new ArrayList<HandleMonitor>();
		HandleMonitor m = new HandleMonitor();
		m.setStartupMessageCount(starts);
		m.setShutdownMessageCount(shutdowns);
		m.setSendMessageCount(sends);
		m.setIndexLastMessage(lastIndex);
		m.setExpectedPayloadSize(expectedPayloadSize);
		list.add(m);
		return list;
	}
	public void assertHandle(int numberInboundMessages, int numberEntitiesProcessed, List<HandleMonitor> expectedMonitors) {
		for (int i = 0; i < expectedMonitors.size(); i++) {
			HandleMonitor expected = expectedMonitors.get(i);
			HandleMonitor actual = messages.get(i).getCallback().getMonitor();
			
			assertEquals("Send message counts do not match", expected.getSendMessageCount(), actual.getSendMessageCount());
			assertEquals("Start message counts do not match", expected.getStartupMessageCount(), actual.getStartupMessageCount());
			assertEquals("Shutdown message counts do not match", expected.getShutdownMessageCount(), actual.getShutdownMessageCount());
			assertEquals("Last message positions do not match", expected.getIndexLastMessage(), actual.getIndexLastMessage());
			TestUtils.assertList(expected.getTargetStepIds(), actual.getTargetStepIds());
			assertEquals("Payload sized unexpected.", expected.getExpectedPayloadSize(), actual.getPayloads().size());
			// TODO add payload assert
		}
	}
	
	/*
	public void assertHandle(int targetMessageCount, int numberInboundMessages,
			int numberOutboundMessages, int numberEntitiesProcessed, boolean unitOfWorkLastMessage) {
		assertHandle(targetMessageCount, numberInboundMessages, numberOutboundMessages, numberEntitiesProcessed, unitOfWorkLastMessage, null, null);
	}
	
	@SuppressWarnings("unchecked")
    public void assertHandle(int numberInboundMessages, int numberEntitiesProcessed, boolean lastMessage, List<String> targetStepIds
			Object entityKey, Object entityValue) {
		
		ISendMessageCallback callback = messages.get(0).getCallback();
		Message inputMessage = messages.get(0).getInputMessage();
		
		//assertEquals("Target message counts are not equal", targetMessageCount, callback..getPayloadList().size());
		assertEquals("Statistics inbound messages are not equal", numberInboundMessages, 
				((AbstractComponentRuntime) spy).getComponentStatistics().getNumberInboundMessages());
		assertEquals("Statistics outbound messages are not equal", numberOutboundMessages, 
				((AbstractComponentRuntime) spy).getComponentStatistics().getNumberOutboundMessages());
		assertEquals("Statistics entities processed are not equal", numberEntitiesProcessed, 
				((AbstractComponentRuntime) spy).getComponentStatistics().getNumberEntitiesProcessed());
		
		if (entityKey != null) {
			assertTrue("Expected entity key " + entityKey + " but there were not any output messages.",
					msgTarget.getPayloadList().size() > 0);
		}
		
		for (int i = 0; i < msgTarget.getPayloadList().size(); i++) {			
			if (i == 0 && entityKey != null) {
				ArrayList<EntityData> actualPayload = (ArrayList<EntityData>)msgTarget.getPayloadList().get(i);
				EntityData actualEntityData = actualPayload.get(i);
				
				assertEquals("Entity value not as expected", entityValue, actualEntityData.get(entityKey));
				
			}
		}
		
	}
	*/
	
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
		HandleParams params = new HandleParams(new Message("inputMessage"));
		messages.add(params);
	}
	
	
}
