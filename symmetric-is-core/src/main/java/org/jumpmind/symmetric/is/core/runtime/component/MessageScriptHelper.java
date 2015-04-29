package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.sql.Row;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;
import org.springframework.jdbc.core.JdbcTemplate;

public class MessageScriptHelper {

	protected IComponent component;
	
	protected FlowStep flowStep;
	
	protected Flow flow;

    protected Iterator<EntityData> entityDataIterator;

    protected Message inputMessage;

    protected IMessageTarget messageTarget;
    
    protected ComponentStatistics componentStatistics;
    
    protected IResource resource;

    public MessageScriptHelper(IComponent component) {
        this.component = component;
        this.flowStep = component.getFlowStep();
        this.flow = component.getFlow();
        this.componentStatistics = component.getComponentStatistics();
        this.resource = component.getResource();
    }

    protected JdbcTemplate getJdbcTemplate() {
        DataSource ds = component.getResource().reference();
        return new JdbcTemplate(ds);
    }

    protected BasicDataSource getBasicDataSource() {
        return (BasicDataSource) component.getResource().reference();
    }

    protected Row nextRowFromInputMessage() {
        if (component.getFlowStep().getComponent().getInputModel() != null) {
            if (entityDataIterator == null) {
                List<EntityData> list = inputMessage.getPayload();
                entityDataIterator = list.iterator();
            }

            if (entityDataIterator.hasNext()) {
                EntityData data = entityDataIterator.next();
                return component.getFlowStep().getComponent().toRow(data);
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException(
                    "The input model needs to be set if you are going to use the entity data");
        }
    }

    protected void info(String message, Object... args) {
        component.getExecutionTracker().log(component.getExecutionId(), LogLevel.INFO, component,
                String.format(message, args));
    }

    protected void setInputMessage(Message inputMessage) {
        this.inputMessage = inputMessage;
    }

    protected void setMessageTarget(IMessageTarget messageTarget) {
        this.messageTarget = messageTarget;
    }

    protected void onInit() {

    }

    protected void onHandle() {
    }

    protected void onError(Throwable myError, List<Throwable> allErrors) {
    }
    
    protected void onSuccess() {
    }


}
