package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
import org.jumpmind.metl.core.runtime.component.HandleParams.MessageTarget;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;
import org.junit.Before;

public abstract class AbstractComponentRuntimeTest {

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
	public abstract void testHandleEmptyPayload();
	public abstract void testHandleUnitOfWorkInputMessage();
	public abstract void testHandleUnitOfWorkFlow();
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
		doNothing().when((AbstractComponentRuntime) spy).sendMessage(any(Serializable.class), any(IMessageTarget.class), anyBoolean());
		
		spy.start(context);
		setupCalled = true;
	}
	
	public void runHandle() {
		if (!setupCalled) {
			setupHandle();
		}
		
		for(int i = 0; i < messages.size(); i++) {
			HandleParams p = messages.get(i);
			spy.handle(p.getInputMessage(), p.getTarget(), p.getUnitOfWorkLastMessage());
		}
	}
	
	public void assertHandle(int targetMessageCount, int numberInboundMessages,
			int numberOutboundMessages, int numberEntitiesProcessed) {
		assertHandle(targetMessageCount, numberInboundMessages, numberOutboundMessages, numberEntitiesProcessed, false, null, null);
	}
	
	public void assertHandle(int targetMessageCount, int numberInboundMessages,
			int numberOutboundMessages, int numberEntitiesProcessed, boolean unitOfWorkLastMessage) {
		assertHandle(targetMessageCount, numberInboundMessages, numberOutboundMessages, numberEntitiesProcessed, unitOfWorkLastMessage, null, null);
	}
	
	public void assertHandle(int targetMessageCount, int numberInboundMessages,
			int numberOutboundMessages, int numberEntitiesProcessed, boolean unitOfWorkLastMessage, 
			Object entityKey, Object entityValue) {
		
		MessageTarget target = messages.get(0).getTarget();
		Message inputMessage = messages.get(0).getInputMessage();
		
		assertEquals("Target message counts are not equal", targetMessageCount, target.getTargetMessageCount());
		assertEquals("Statistics inbound messages are not equal", numberInboundMessages, 
				((AbstractComponentRuntime) spy).getComponentStatistics().getNumberInboundMessages());
		assertEquals("Statistics outbound messages are not equal", numberOutboundMessages, 
				((AbstractComponentRuntime) spy).getComponentStatistics().getNumberOutboundMessages());
		assertEquals("Statistics entities processed are not equal", numberEntitiesProcessed, 
				((AbstractComponentRuntime) spy).getComponentStatistics().getNumberEntitiesProcessed());
		
		if (entityKey != null) {
			assertTrue("Expected entity key " + entityKey + " but there were not any output messages.",
					target.getTargetMessageCount() > 0);
		}
		
		for (int i = 0; i < target.getTargetMessageCount(); i++) {
			assertEquals("Sequence numbers are not equal", inputMessage.getHeader().getSequenceNumber(), 
					target.getMessage(i).getHeader().getSequenceNumber()); 
			assertEquals("Unit of work not equal", unitOfWorkLastMessage, 
					target.getMessage(i).getHeader().isUnitOfWorkLastMessage());
			
			if (i == 0 && entityKey != null) {
				ArrayList<EntityData> actualPayload = target.getMessage(i).getPayload();
				EntityData actualEntityData = actualPayload.get(i);
				
				assertEquals("Entity value not as expected", entityValue, actualEntityData.get(entityKey));
				
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
		HandleParams params = new HandleParams(new Message("inputMessage"));
		messages.add(params);
	}
	
	
}
