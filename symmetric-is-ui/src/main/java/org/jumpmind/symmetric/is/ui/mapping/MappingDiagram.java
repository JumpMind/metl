package org.jumpmind.symmetric.is.ui.mapping;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.runtime.component.MappingProcessor;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

@JavaScript({ "dom.jsPlumb-1.7.4-min.js", "mapping-diagram.js" })
@StyleSheet({ "mapping-diagram.css" })
@SuppressWarnings("serial")
public class MappingDiagram extends AbstractJavaScriptComponent {

	ApplicationContext context;

	Component component;
	
    String selectedSourceId;
    
    String selectedTargetId;

	public MappingDiagram(ApplicationContext context, Component component) {
		this.context = context;
		this.component = component;
        setPrimaryStyleName("mapping-diagram");
        setId("mapping-diagram");

        MappingDiagramState state = getState();
        state.component = component;
        state.inputModel = component.getInputModel();
        state.outputModel = component.getOutputModel();

        addFunction("onSelect", new OnSelectFunction());       
        addFunction("onConnection", new OnConnectionFunction());
    }
    
    @Override
    protected MappingDiagramState getState() {
        return (MappingDiagramState) super.getState();
    }

    public void removeSelected() {
    	if (selectedSourceId != null) {
    		removeConnection(selectedSourceId, selectedTargetId);
    		selectedSourceId = selectedTargetId = null;
    		markAsDirty();
    	}
    }

    protected void removeConnection(String sourceId, String targetId) {
    	List<ComponentAttributeSetting> settings = component.getAttributeSetting(sourceId, MappingProcessor.ATTRIBUTE_MAPS_TO);
    	for (ComponentAttributeSetting setting : settings) {
    		if (setting.getValue().equals(targetId)) {
        		component.getAttributeSettings().remove(setting);
        		context.getConfigurationService().delete(setting);
    		}
    	}
    }
    
    class OnSelectFunction implements JavaScriptFunction {
    	public void call(JsonArray arguments) {
            if (arguments.length() > 0) {
                JsonObject json = arguments.getObject(0);
                selectedSourceId = json.getString("sourceId").substring(3);
                selectedTargetId = json.getString("targetId").substring(3);
            } else {
            	selectedSourceId = selectedTargetId = null;
            }
            fireEvent(new SelectEvent(MappingDiagram.this, selectedSourceId, selectedTargetId));
    	}
    }

    class OnConnectionFunction implements JavaScriptFunction {
		public void call(JsonArray arguments) {
            if (arguments.length() > 0) {
                JsonObject json = arguments.getObject(0);
                String sourceId = json.getString("sourceId").substring(3);
                String targetId = json.getString("targetId").substring(3);
                boolean removed = json.getBoolean("removed");
                if (removed) {
                	removeConnection(sourceId, targetId);
                } else {
                	ComponentAttributeSetting setting = new ComponentAttributeSetting();
                	setting.setAttributeId(sourceId);
                	setting.setComponentId(component.getId());
            		setting.setName(MappingProcessor.ATTRIBUTE_MAPS_TO);
            		component.addAttributeSetting(setting);
                	setting.setValue(targetId);
                	context.getConfigurationService().save(setting);
                }
            }
		}
    }
}
