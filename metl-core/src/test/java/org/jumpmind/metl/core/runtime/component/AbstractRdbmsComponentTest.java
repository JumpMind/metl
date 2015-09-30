package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.MessageManipulationStrategy;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public abstract class AbstractRdbmsComponentTest extends AbstractComponentRuntimeTest {

	List<String> sqls;
	String expectedFlowReplacementSql;
	Map<String, Object> expectedParamMap;
	long rowsPerMessage;
    MessageManipulationStrategy messageManipulationStrategy;
    boolean trimColumns;
    boolean matchOnColumnNameOnly;

    
	@Before
	public void reset() {
		super.reset();
		sqls = new ArrayList<String>();
		expectedFlowReplacementSql = "";
		expectedParamMap = new HashMap<String, Object>();
		rowsPerMessage = 0L;
	    messageManipulationStrategy = MessageManipulationStrategy.REPLACE;
	    trimColumns = false;
	    matchOnColumnNameOnly = false;
	}
	
	public void setupHandle() {
		
		super.setupHandle();
		
		NamedParameterJdbcTemplate mJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		
		// Verify flow parameters as a string map were replaced
		if (flowParametersAsString != null && flowParametersAsString.size() > 0) {
			when(mJdbcTemplate.query(anyString(), anyMap(), Mockito.any(ResultSetExtractor.class))).thenAnswer(new Answer() {
				public Object answer(InvocationOnMock invocation) {
					Object[] args = invocation.getArguments();
					
					// Verify flow parameters were replaced
					assertEquals(expectedFlowReplacementSql, args[0]);
					
					return resultMessage;
				}
			});
		}
		// Verify flow parameters as a serialized map were set
		else if (flowParameters != null && flowParameters.size() > 0) {
			when(mJdbcTemplate.query(anyString(), anyMap(), Mockito.any(ResultSetExtractor.class))).thenAnswer(new Answer() {
				public Object answer(InvocationOnMock invocation) {
					Object[] args = invocation.getArguments();
					
					
					
					if (((Map) args[1]).size() > 0) {
						Map<String, Object> paramMap = (Map<String, Object>) args[1];
						for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
							Map<String, Object> expectedMap = expectedParamMap;
							assertTrue(expectedMap.containsKey(entry.getKey()));
							
							Object expectedValue = expectedMap.get(entry.getKey());
							Object actualValue = entry.getValue();
							
							assertEquals("Param map to be used with sql executed does not match for key : " + entry.getKey(), expectedValue, actualValue);
						}
					}
					return resultMessage;
				}
			});
		} else {
			when(mJdbcTemplate.query(anyString(), anyMap(), Mockito.any(ResultSetExtractor.class))).thenReturn(inputMessage);
		}
		doReturn(mJdbcTemplate).when((AbstractRdbmsComponent) spy).getJdbcTemplate();
		
	}
}
