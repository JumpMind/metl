package org.jumpmind.symmetric.is.ui.mapping;

import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
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
	
	public MappingDiagram(ApplicationContext context, Model inputModel, Model outputModel) {
		this.context = context;
        setPrimaryStyleName("mapping-diagram");
        setId("mapping-diagram");

        MappingDiagramState state = getState();
        state.inputModel = inputModel;
        state.outputModel = outputModel;
        
        addFunction("onConnection", new OnConnectionFunction());
    }
    
    @Override
    protected MappingDiagramState getState() {
        return (MappingDiagramState) super.getState();
    }

    class OnConnectionFunction implements JavaScriptFunction {
		public void call(JsonArray arguments) {
			System.out.println("onConnection");
            if (arguments.length() > 0) {
                JsonObject json = arguments.getObject(0);
                String sourceId = json.getString("sourceId").substring(3);
                String targetId = json.getString("targetId").substring(3);
                boolean removed = json.getBoolean("removed");
                System.out.println("Connection " + sourceId + " -> " + targetId + " (" + removed + ")");
                ModelAttribute sourceAttr = getModelAttribute(sourceId);
                ModelAttribute targetAttr = getModelAttribute(targetId);
                System.out.println("Found source: " + sourceAttr);
                System.out.println("Found target: " + targetAttr);
            }
		}
		
		ModelAttribute getModelAttribute(String id) {
            for (ModelEntity entity : getState().inputModel.getModelEntities()) {
            	for (ModelAttribute attr : entity.getModelAttributes()) {
            		if (id.equals(attr.getId())) {
            			return attr;
            		}
            	}
            }
            return null;
		}
    }
}
