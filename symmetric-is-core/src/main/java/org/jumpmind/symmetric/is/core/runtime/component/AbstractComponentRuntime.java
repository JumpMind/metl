package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceRuntime;

abstract public class AbstractComponentRuntime extends AbstractRuntimeObject implements IComponentRuntime {

    @SettingDefinition(order = 100, required = false, type = Type.INTEGER, defaultValue = "10000", label = "Inbound Queue Capacity")
    public final static String INBOUND_QUEUE_CAPACITY = "inbound.queue.capacity";
    
    @SettingDefinition(order = 0, required = false, type = Type.BOOLEAN, defaultValue = "true", label = "Enabled")
    public final static String ENABLED = "enabled";

    protected ComponentContext context;
    protected boolean enabled = true;
    protected boolean shared = false;

    @Override
    public void start() {        
    }

    @Override
    public void lastMessageReceived(IMessageTarget messageTarget) {
        
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
        
    public void init(ComponentContext context) {
        this.context  = context;
    }
    
    @Override
    public void flowCompleted() {
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
    
    protected void log(LogLevel level, String msg, Object... args) {
        getExecutionTracker().log(level, this.getComponentContext(), msg, args);
    }
    
    protected Flow getFlow() {
        return context.getFlow();
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
    
}
