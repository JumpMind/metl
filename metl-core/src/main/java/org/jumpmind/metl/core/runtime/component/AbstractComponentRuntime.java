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
import org.jumpmind.metl.core.runtime.AbstractRuntimeObject;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.IExecutionTracker;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.ComponentUtil;
import org.jumpmind.properties.TypedProperties;

abstract public class AbstractComponentRuntime extends AbstractRuntimeObject implements IComponentRuntime {

    public final static String INBOUND_QUEUE_CAPACITY = "inbound.queue.capacity";     
    
    public final static String ENABLED = "enabled";

    protected ComponentContext context;
    
    protected int threadNumber;
    
    protected XMLComponent componentDefinition;   
    
    @Override
    public void register(XMLComponent definition) {
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
        start();
    }
    
    abstract protected void start();
    
    public TypedProperties getTypedProperties() {
        List<XMLSetting> settings = componentDefinition != null ? componentDefinition.getSettings().getSetting() : null;
        if (settings == null) {
            settings = Collections.emptyList();
        }
        return getComponent().toTypedProperties(settings);
    }
    
    public void stop() {
    }
    
    @Override
    public ComponentContext getComponentContext() {
        return context;
    }
    
    protected ComponentStatistics getComponentStatistics() {
    	return context.getComponentStatistics();
    }
            
    @Override
    public void flowCompleted(boolean cancelled) {
    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError) {
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
        return context.getResourceRuntime();
    }
    
    protected <T> T getResourceReference() {
        return context.getResourceRuntime().reference();
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
    
    protected Bindings bindEntityData(ScriptEngine scriptEngine, EntityData entityData) {
        Bindings bindings = scriptEngine.createBindings();
        Model model = context.getFlowStep().getComponent().getInputModel();
        List<ModelEntity> entities = model.getModelEntities();
        for (ModelEntity modelEntity : entities) {
            HashMap<String, Object> boundEntity = new HashMap<String, Object>();
            bindings.put(modelEntity.getName(), boundEntity);
        }

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

    protected Bindings bindStringData(ScriptEngine scriptEngine, String value) {
        Bindings bindings = scriptEngine.createBindings();
        if (value != null) {
        	bindings.put("text", value);
        } else {
            log(LogLevel.WARN, "Binding was unsuccessful since the value was null");
        }
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return bindings;
    }

    
    
    protected Object getAttributeValue(Message inputMessage, String entityName, String attributeName) {
        ArrayList<EntityData> rows = inputMessage.getPayload();
        return ComponentUtil.getAttributeValue(getInputModel(), rows, entityName, attributeName);
    }

    protected List<Object> getAttributeValues(Message inputMessage, String entityName, String attributeName) {
        ArrayList<EntityData> rows = inputMessage.getPayload();
        return ComponentUtil.getAttributeValues(getInputModel(), rows, entityName, attributeName);
    }    
    
}
