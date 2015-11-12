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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.ComponentUtil;
import org.jumpmind.metl.core.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class MessageScriptHelper {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    
	protected ComponentContext context;
	
	protected FlowStep flowStep;
	
	protected Flow flow;

    protected Iterator<EntityData> entityDataIterator;

    protected Message inputMessage;

    @Deprecated
    protected ISendMessageCallback messageTarget;
    
    protected ISendMessageCallback callback;
    
    protected ComponentStatistics componentStatistics;
    
    protected IResourceRuntime resource;

    protected Map<String, Object> scriptContext;
    
    protected boolean unitOfWorkBoundaryReached;
    
    public MessageScriptHelper(IComponentRuntime component) {
        this.context = component.getComponentContext();
        this.resource = context.getResourceRuntime();
        this.componentStatistics = context.getComponentStatistics();
        this.flow = context.getManipulatedFlow();
        this.flowStep = context.getFlowStep();
        this.scriptContext = new HashMap<String, Object>(); 
    }

    protected JdbcTemplate getJdbcTemplate() {
        if (resource == null) {
            throw new IllegalStateException("In order to create a jdbc template, a datasource resource must be defined");
        }
        DataSource ds = resource.reference();
        return new JdbcTemplate(ds);
    }

    protected BasicDataSource getBasicDataSource() {
        return (BasicDataSource) resource.reference();
    }

    protected Row nextRowFromInputMessage() {
        if (flowStep.getComponent().getInputModel() != null) {
            if (entityDataIterator == null) {
                List<EntityData> list = inputMessage.getPayload();
                entityDataIterator = list.iterator();
            }

            if (entityDataIterator.hasNext()) {
                EntityData data = entityDataIterator.next();
                return flowStep.getComponent().toRow(data, false, true);
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException(
                    "The input model needs to be set if you are going to use the entity data");
        }
    }

    protected void info(String message, Object... args) {        
        context.getExecutionTracker().log(ThreadUtils.getThreadNumber(), LogLevel.INFO, context, message, args);
    }

    protected void setInputMessage(Message inputMessage) {
        this.inputMessage = inputMessage;
    }
    
    protected void setUnitOfWorkBoundaryReached(boolean unitOfWorkBoundaryReached) {
    	this.unitOfWorkBoundaryReached = unitOfWorkBoundaryReached;
    }
    
    protected boolean containsEntity(String entityName, EntityData data) {
        return flowStep.getComponent().getEntityNames(data, true).contains(entityName);
    }
    
    protected void putAttributeValue(String entityName, String attributeName, EntityData data, Object value) {
        ModelAttribute attribute = flowStep.getComponent().getInputModel().getAttributeByName(entityName, attributeName);
        data.put(attribute.getId(), value);
    }
    
    protected Object getAttributeValue(String entityName, String attributeName, EntityData data) {
        Model model = flowStep.getComponent().getInputModel();
        return ComponentUtil.getAttributeValue(model, data, entityName, attributeName);
    }

    protected Object getAttributeValue(String entityName, String attributeName) {
        Model model = flowStep.getComponent().getInputModel();
        ArrayList<EntityData> rows = inputMessage.getPayload();
        return ComponentUtil.getAttributeValue(model, rows, entityName, attributeName);
    }
    
    protected Object getAttributeValue(String attributeName, EntityData data) {
        Model model = flowStep.getComponent().getInputModel();
        return ComponentUtil.getAttributeValue(model, data, attributeName);
    }

    protected List<Object> getAttributeValues(String entityName, String attributeName) {
        Model model = flowStep.getComponent().getInputModel();
        ArrayList<EntityData> rows = inputMessage.getPayload();
        return ComponentUtil.getAttributeValues(model, rows, entityName, attributeName);
    }
    
    protected void forwardMessageWithParameter(String parameterName, Serializable value) {
        Map<String, Serializable> headers = new HashMap<>();
        headers.put(parameterName, value);
        callback.sendMessage(headers, inputMessage.getPayload());
    }
    
    protected void forwardMessageWithParameters(Map<String,Serializable> params) {
        callback.sendMessage(params, inputMessage.getPayload());
    }
    
    protected void forwardMessage() {
        callback.sendMessage(null, inputMessage.getPayload());
    }
    
    protected void sendControlMessage() {
        callback.sendControlMessage(inputMessage.getHeader());
    }
    
    protected void setSendMessageCallback(ISendMessageCallback callback) {
        this.messageTarget = callback;
        this.callback = callback;
    }

    protected Map<String, Object> getScriptContext() {
		return scriptContext;
	}

	protected void onInit() {
    }

    protected void onHandle() {        
    }

    protected void onError(Throwable myError) {
    }
    
    protected void onSuccess() {
    }

}
