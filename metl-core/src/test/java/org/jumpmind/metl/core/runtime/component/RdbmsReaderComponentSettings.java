package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.MessageManipulationStrategy;

public class RdbmsReaderComponentSettings extends ComponentSettings {
	List<String> sqls;

	String expectedFlowReplacementSql;
	Map<String, Object> expectedParamMap;
	
    long rowsPerMessage;

    MessageManipulationStrategy messageManipulationStrategy;

    boolean trimColumns;

    boolean matchOnColumnNameOnly;

    public RdbmsReaderComponentSettings() {
		this.sqls = new ArrayList<String>();
		this.messageManipulationStrategy = MessageManipulationStrategy.REPLACE;
	}
    
	List<String> getSqls() {
		return sqls;
	}

	void setSqls(List<String> sqls) {
		this.sqls = sqls;
	}

	long getRowsPerMessage() {
		return rowsPerMessage;
	}

	void setRowsPerMessage(long rowsPerMessage) {
		this.rowsPerMessage = rowsPerMessage;
	}

	MessageManipulationStrategy getMessageManipulationStrategy() {
		return messageManipulationStrategy;
	}

	void setMessageManipulationStrategy(MessageManipulationStrategy messageManipulationStrategy) {
		this.messageManipulationStrategy = messageManipulationStrategy;
	}

	boolean isTrimColumns() {
		return trimColumns;
	}

	void setTrimColumns(boolean trimColumns) {
		this.trimColumns = trimColumns;
	}

	boolean isMatchOnColumnNameOnly() {
		return matchOnColumnNameOnly;
	}

	void setMatchOnColumnNameOnly(boolean matchOnColumnNameOnly) {
		this.matchOnColumnNameOnly = matchOnColumnNameOnly;
	}

	String getExpectedFlowReplacementSql() {
		return expectedFlowReplacementSql;
	}

	void setExpectedFlowReplacementSql(String expectedFlowReplacementSql) {
		this.expectedFlowReplacementSql = expectedFlowReplacementSql;
	}

	Map<String, Object> getExpectedParamMap() {
		return expectedParamMap;
	}

	void setExpectedParamMap(Map<String, Object> expectedParamMap) {
		this.expectedParamMap = expectedParamMap;
	}

	
    
}
