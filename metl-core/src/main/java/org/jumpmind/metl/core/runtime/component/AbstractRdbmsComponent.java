package org.jumpmind.metl.core.runtime.component;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jumpmind.db.sql.SqlScriptReader;
import org.jumpmind.metl.core.runtime.Message;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;


abstract public class AbstractRdbmsComponent extends AbstractComponentRuntime {
	
	protected List<Result> results = new ArrayList<Result>();
	
    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        return new NamedParameterJdbcTemplate((DataSource) this.context.getResourceRuntime().reference());
    }
    
    protected List<String> getSqlStatements(String script) {
        List<String> sqlStatements = new ArrayList<String>();
        SqlScriptReader scriptReader = new SqlScriptReader(new StringReader(script));
        try {
            String sql = scriptReader.readSqlStatement();
            while (sql != null) {
                sqlStatements.add(sql);
                sql = scriptReader.readSqlStatement();
            }
            return sqlStatements;
        } finally {
            IOUtils.closeQuietly(scriptReader);
        }
    }
        
    protected ArrayList<String> convertResultsToTextPayload(List<Result> results) {
    	ArrayList<String> payload = new ArrayList<String>();
    	JSONArray jsonResults = new JSONArray();
    	for (Result result:results) {
    		JSONObject jsonResult = new JSONObject();
    		jsonResult.put("Sql", result.sql);
    		jsonResult.put("Rows Affected", result.numberRowsAffected);
    		jsonResults.add(jsonResult);
    	}
    	payload.add(jsonResults.toJSONString());
    	return payload;
    }
    
    protected Message createResultMessage(Message inputMessage, List<Result> results, boolean unitOfWorkLastMessage) {
        Message resultMessage = new Message(getFlowStepId());
        if (inputMessage.getHeader().isUnitOfWorkLastMessage()) {
        	resultMessage.getHeader().setUnitOfWorkLastMessage(true);
        }
        resultMessage.setPayload(convertResultsToTextPayload(results));
        if (unitOfWorkLastMessage) {
        	resultMessage.getHeader().setUnitOfWorkLastMessage(true);
        }
        return resultMessage;
    }
    
    class Result {
    	String sql;
    	int numberRowsAffected;

    	Result (String sql, int numberRowsAffected) {
    		this.sql = sql;
    		this.numberRowsAffected = numberRowsAffected;
    	}
    }

}
