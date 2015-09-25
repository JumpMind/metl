package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;

public class AbstractComponentRuntimeTest {

	public void setupHandle(AbstractComponentRuntime spy, ComponentSettings settings) {
		ComponentContext mContext = mock(ComponentContext.class);
		ComponentStatistics eComponentStatistics = new ComponentStatistics();
		ExecutionTrackerNoOp eExecutionTracker = new ExecutionTrackerNoOp();
		
		when(mContext.getComponentStatistics()).thenReturn(eComponentStatistics);
		when(mContext.getFlowParametersAsString()).thenReturn(settings.getFlowParametersAsString());
		when(mContext.getFlowParameters()).thenReturn(settings.getFlowParameters());
		
		doReturn(eExecutionTracker).when(spy).getExecutionTracker();
		
		spy.start(mContext);
		
	}
}
