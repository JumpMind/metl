package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;

abstract public class AbstractComponent extends AbstractRuntimeObject implements IComponent {

    @SettingDefinition(order = 100, required = false, type = Type.INTEGER, defaultValue = "10000", label = "Inbound Queue Capacity")
    public final static String INBOUND_QUEUE_CAPACITY = "inbound.queue.capacity";
    
    @SettingDefinition(order = 0, required = false, type = Type.BOOLEAN, defaultValue = "true", label = "Enabled")
    public final static String ENABLED = "enabled";

    protected Flow flow;
    protected FlowStep flowStep; 
    protected Map<String, IResource> resources;
    protected IResource resource;
    protected IExecutionTracker executionTracker;
    protected ComponentStatistics componentStatistics;
    protected String executionId;
    protected boolean enabled = true;
    protected boolean shared = false;

    @Override
    public void start(String executionId, IExecutionTracker executionTracker) {
        this.componentStatistics = new ComponentStatistics();
    	this.executionTracker = executionTracker;
    	this.executionId = executionId;
    }

    public void stop() {
    }
    
    @Override
    public String getExecutionId() {
        return executionId;
    }
    
    @Override
    public ComponentStatistics getComponentStatistics() {
    	return componentStatistics;
    }
    
    @Override
    public FlowStep getFlowStep() {
    	return this.flowStep;
    }
    
    @Override
    public IResource getResource() {
        return resource;
    }
    
    @Override
    public IExecutionTracker getExecutionTracker() {
        return executionTracker;
    }
    
    @Override
    public Flow getFlow() {
        return flow;
    }
    
    public void init(FlowStep flowStep, Flow flow, Map<String, IResource> resources) {
    	this.flowStep = flowStep;
    	this.flow = flow;
    	this.resources = resources;
    	Resource r = flowStep.getComponent().getResource();
    	if (r != null) {
    	    resource = resources.get(r.getId());
    	}
    }
    
    @Override
    public void flowCompletedWithoutError() {
    }
    
    @Override
    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors) {
    }
    
    protected Bindings bindEntityData(ScriptEngine scriptEngine, String executionId, EntityData entityData) {
        Bindings bindings = scriptEngine.createBindings();
        Model model = flowStep.getComponent().getInputModel();
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
                executionTracker.log(executionId, LogLevel.WARN, this,
                        "Could not find attribute in the input model with an id of " + attributeId);
            }
        }
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return bindings;
    }
    
}
