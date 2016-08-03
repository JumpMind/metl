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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.IExecutionTracker;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.definition.ComponentSettingsConstants;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.ComponentUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractComponentRuntime implements IComponentRuntime {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public final static String INBOUND_QUEUE_CAPACITY = ComponentSettingsConstants.INBOUND_QUEUE_CAPACITY;   
    
    public final static String ENABLED = ComponentSettingsConstants.ENABLED;

    public final static String LOG_INPUT = ComponentSettingsConstants.LOG_INPUT;

    public final static String LOG_OUTPUT = ComponentSettingsConstants.LOG_OUTPUT;
    
    public final static String ROWS_PER_MESSAGE = ComponentSettingsConstants.ROWS_PER_MESSAGE;
    
    public final static String RUN_WHEN = ComponentSettingsConstants.RUN_WHEN;
    
    public static final String PER_UNIT_OF_WORK = ComponentSettingsConstants.PER_UNIT_OF_WORK;

    public static final String PER_MESSAGE = ComponentSettingsConstants.PER_MESSAGE;

    public static final String PER_ENTITY = ComponentSettingsConstants.PER_ENTITY;

    protected ComponentContext context;
    
    protected int threadNumber;
    
    protected XMLComponent componentDefinition;   
    
    protected TypedProperties properties;
    
    @Override
    public void create(XMLComponent definition) {
        this.componentDefinition = definition;
    }
    
    @Override
    public XMLComponent getComponentDefintion() {
        return componentDefinition;
    }

    @Override
    final public void start(int threadNumber, ComponentContext context) {
        this.context = context;
        this.threadNumber = threadNumber;
        this.properties = getTypedProperties();
        start();
    }
    
    abstract protected void start();
    
    protected TypedProperties getTypedProperties() {
        List<XMLSetting> settings = componentDefinition != null ? componentDefinition.getSettings().getSetting() : null;
        if (settings == null) {
            settings = Collections.emptyList();
        }
        return getComponent().toTypedProperties(settings);
    }
    
    @Override
    public void stop() {
    }
    
    @Override
    public ComponentContext getComponentContext() {
        return context;
    }

    @Override
    public void flowCompleted(boolean cancelled) {
    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError) {
    }
    
    protected ComponentStatistics getComponentStatistics() {
    	return context.getComponentStatistics();
    }
            
    protected String getFlowStepId() {
        return context.getFlowStep().getId();
    }
    
    protected FlowStep getFlowStep() {
        return context.getFlowStep();
    }
    
    protected Component getComponent() {
        return context.getFlowStep().getComponent();
    }
    
    protected IResourceRuntime getResourceRuntime() {
        Resource resource = getComponent().getResource();
        if (resource != null) {
           return context.getDeployedResources().get(resource.getId());
        } else {
            return null;
        }
    }
    
    protected String resolveParamsAndHeaders(String text, Message inputMessage) {
        Map<String,String> parms = new HashMap<>(getComponentContext().getFlowParameters());
        parms.putAll(inputMessage.getHeader().getAsStrings());
        return FormatUtils.replaceTokens(text, parms, true);
    }
    
    protected String resolveFlowParams(String text) {
        Map<String,String> parms = new HashMap<>(getComponentContext().getFlowParameters());
        return FormatUtils.replaceTokens(text, parms, true);        
    }
    
    protected <T> T getResourceReference() {
        IResourceRuntime resource = getResourceRuntime();
        if (resource != null) {
            return getResourceRuntime().reference();
        } else {
            return null;
        }
    }
    
    protected Model getOutputModel() {
        return context.getFlowStep().getComponent().getOutputModel();
    }
    
    protected Model getInputModel() {
        return context.getFlowStep().getComponent().getInputModel();
    }

    protected IExecutionTracker getExecutionTracker() {
        return context.getExecutionTracker();
    }
    
    protected void debug(String msg, Object...args) {
        log(LogLevel.DEBUG, msg, args);
    }
    
    protected void info(String msg, Object...args) {
        log(LogLevel.INFO, msg, args);
    }
    
    protected void error(String msg, Object...args) {
        log(LogLevel.ERROR, msg, args);
    }
    
    protected void warn(String msg, Object...args) {
        log(LogLevel.WARN, msg, args);
    }
    
    protected void log(LogLevel level, String msg, Object... args) {
        getExecutionTracker().log(threadNumber, level, this.getComponentContext(), msg, args);
    }
    
    protected Flow getFlow() {
        return context.getManipulatedFlow();
    }
    
    protected void bindHeadersAndFlowParameters(Bindings bindings, Message inputMessage) {
        bindModelEntities(bindings);        
        
        Set<String> messageHeaderKeys = inputMessage.getHeader().keySet();
        for (String messageHeaderKey : messageHeaderKeys) {
            bindings.put(messageHeaderKey, inputMessage.getHeader().get(messageHeaderKey));
        }
        
        Map<String, String> flowParameters = context.getFlowParameters();
        for (String key : flowParameters.keySet()) {
            bindings.put(key, flowParameters.get(key));            
        }
        
        bindings.put("text", null);
        bindings.put("CHANGE_TYPE", null);
        bindings.put("ENTITY_NAMES", new ArrayList<>());  
    }
    
    protected void bindModelEntities(Bindings bindings) {
        Model model = context.getFlowStep().getComponent().getInputModel();
        if (model != null) {
            List<ModelEntity> entities = model.getModelEntities();
            for (ModelEntity modelEntity : entities) {
                HashMap<String, Object> boundEntity = new HashMap<String, Object>(0);
                bindings.put(modelEntity.getName(), boundEntity);
                
                List<ModelAttribute> attributes = modelEntity.getModelAttributes();
                for (ModelAttribute modelAttribute : attributes) {
                    boundEntity.put(modelAttribute.getName(), null);
                }
            }
        }
    }
    
    protected Bindings bindEntityData(ScriptEngine scriptEngine, Message inputMessage, EntityData entityData) {
        Bindings bindings = scriptEngine.createBindings();       
        bindHeadersAndFlowParameters(bindings, inputMessage);
        Model model = getInputModel();
        bindings.put("CHANGE_TYPE", entityData.getChangeType().name());
        bindings.put("ENTITY_NAMES", context.getFlowStep().getComponent().getEntityNames(entityData, true));                
        Set<String> attributeIds = entityData.keySet();
        for (String attributeId : attributeIds) {
            ModelAttribute attribute = model.getAttributeById(attributeId);
            if (attribute != null) {
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                Object value = entityData.get(attributeId);
                @SuppressWarnings("unchecked")
                Map<String, Object> boundEntity = (Map<String, Object>) bindings.get(entity
                        .getName());
                boundEntity.put(attribute.getName(), value);
            } else {
                log(LogLevel.WARN, "Could not find attribute in the input model with an id of " + attributeId);
            }
        }
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return bindings;
    }

    protected Bindings bindStringData(ScriptEngine scriptEngine, Message inputMessage, String value) {
        Bindings bindings = scriptEngine.createBindings();
        bindHeadersAndFlowParameters(bindings, inputMessage);
        if (value != null) {
        	bindings.put("text", value);
        } else {
            log(LogLevel.WARN, "Binding was unsuccessful since the value was null");
        }
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return bindings;
    }

    protected Object getAttributeValue(EntityDataMessage inputMessage, String entityName, String attributeName) {
        ArrayList<EntityData> rows = inputMessage.getPayload();
        return ComponentUtils.getAttributeValue(getInputModel(), rows, entityName, attributeName);
    }

    protected List<Object> getAttributeValues(EntityDataMessage inputMessage, String entityName, String attributeName) {
        ArrayList<EntityData> rows = inputMessage.getPayload();
        return ComponentUtils.getAttributeValues(getInputModel(), rows, entityName, attributeName);
    }
    
    public void setComponentDefinition(XMLComponent componentDefinition) {
        this.componentDefinition = componentDefinition;
    }
    
    public void setContext(ComponentContext context) {
        this.context = context;
    }
    
    public void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }
    
    @Override
    public int getThreadNumber() {
        return this.threadNumber;
    }
    
}