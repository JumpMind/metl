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
import java.util.Map;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.metl.core.runtime.Message;
import org.junit.Before;

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
	public abstract void testHandleShutdownMessage();
	public abstract void testHandleEmptyPayload();
	public abstract void testHandleUnitOfWorkInputMessage();
	public abstract void testHandleUnitOfWorkFlow();
	public abstract void testHandleNormal();
	
	public abstract IComponentRuntime getComponentSpy();
	
	IComponentRuntime spy;
	Message inputMessage;
	Message resultMessage;
	SendMessageCallback<T> msgTarget;
	boolean unitOfWorkLastMessage;
	
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
		inputMessage = new Message("test");
		resultMessage = new Message("");
		msgTarget = new SendMessageCallback<T>();
		spy = getComponentSpy();
		unitOfWorkLastMessage = false;
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
		
		spy.handle(inputMessage, msgTarget, unitOfWorkLastMessage);
	}
	
	public void assertHandle(int targetMessageCount, int numberInboundMessages,
			int numberOutboundMessages, int numberEntitiesProcessed) {
		assertHandle(targetMessageCount, numberInboundMessages, numberOutboundMessages, numberEntitiesProcessed, false, null, null);
	}
	
	public void assertHandle(int targetMessageCount, int numberInboundMessages,
			int numberOutboundMessages, int numberEntitiesProcessed, boolean unitOfWorkLastMessage) {
		assertHandle(targetMessageCount, numberInboundMessages, numberOutboundMessages, numberEntitiesProcessed, unitOfWorkLastMessage, null, null);
	}
	
	@SuppressWarnings("unchecked")
    public void assertHandle(int targetMessageCount, int numberInboundMessages,
			int numberOutboundMessages, int numberEntitiesProcessed, boolean unitOfWorkLastMessage, 
			Object entityKey, Object entityValue) {
		
		assertEquals("Target message counts are not equal", targetMessageCount, msgTarget.getPayloadList().size());
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

}
