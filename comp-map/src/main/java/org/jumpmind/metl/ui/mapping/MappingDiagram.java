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
package org.jumpmind.metl.ui.mapping;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentModelSetting;
import org.jumpmind.metl.core.model.HierarchicalModel;
import org.jumpmind.metl.core.model.ModelEntitySorter;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.ComponentModelSetting.Type;
import org.jumpmind.metl.core.runtime.component.Mapping;
import org.jumpmind.metl.ui.common.ApplicationContext;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

@JavaScript({ "dom.jsPlumb-1.7.5-min.js", "mapping-diagram.js" })
@StyleSheet({ "mapping-diagram.css" })
@SuppressWarnings("serial")
public class MappingDiagram extends AbstractJavaScriptComponent {

	ApplicationContext context;

	Component component;

	String selectedSourceId;

	String selectedTargetId;

	boolean readOnly;

	public MappingDiagram(ApplicationContext context, Component component, boolean readOnly) {
		this.context = context;
		this.component = component;
		this.readOnly = readOnly;
		setPrimaryStyleName("mapping-diagram");
		setId("mapping-diagram");

		cleanAbandonedLinks(component);

		MappingDiagramState state = getState();
		state.component = component;
		state.readOnly = readOnly;

        if (component.getInputModel() instanceof RelationalModel) {
            state.relationalInputModel = (RelationalModel) component.getInputModel();
            context.getConfigurationService().refresh((RelationalModel) state.relationalInputModel);
            Collections.sort(((RelationalModel) state.relationalInputModel).getModelEntities(), new ModelEntitySorter());
            ((RelationalModel) state.relationalInputModel).sortAttributes();
        } else {
            state.hierarchicalInputModel = (HierarchicalModel) component.getInputModel();
            context.getConfigurationService().refresh((HierarchicalModel) state.hierarchicalInputModel);
        }

        if (component.getOutputModel() instanceof RelationalModel) {
            state.relationalOutputModel = (RelationalModel) component.getOutputModel();
            if (state.relationalOutputModel != null) {
                context.getConfigurationService().refresh((RelationalModel) state.relationalOutputModel);
                Collections.sort(((RelationalModel) state.relationalOutputModel).getModelEntities(), new ModelEntitySorter());
                ((RelationalModel) state.relationalOutputModel).sortAttributes();
            }
        } else {
            state.hierarchicalOutputModel = (HierarchicalModel) component.getOutputModel();     
            context.getConfigurationService().refresh((HierarchicalModel) state.hierarchicalOutputModel);            
        }		
		addFunction("onSelect", new OnSelectFunction());
		addFunction("onConnection", new OnConnectionFunction());
	}

    protected void cleanAbandonedLinks(Component c) {
        if (c.getModelSettings() != null) {
            Iterator<ComponentModelSetting> iter = c.getModelSettings().iterator();
            boolean elementsExist = true;
            while (iter.hasNext()) {
                ComponentModelSetting setting = iter.next();
                if (Mapping.MODEL_OBJECT_MAPS_TO.equals(setting.getName())) {
                    if (c.getInputModel() instanceof RelationalModel) {
                        RelationalModel inputModel = (RelationalModel) c.getInputModel();
                        if (inputModel.getAttributeById(setting.getModelObjectId()) == null &&
                                inputModel.getEntityById(setting.getModelObjectId()) == null) {
                            elementsExist = false;
                        }
                    } else {
                        HierarchicalModel inputModel = (HierarchicalModel) c.getInputModel();
                        if (inputModel.getObjectById(setting.getModelObjectId()) == null) {
                            elementsExist = false;
                        }
                    }
                    
                    if (elementsExist) {
                        if (c.getOutputModel() instanceof RelationalModel) {
                            RelationalModel outputModel = (RelationalModel) c.getOutputModel();
                            if (outputModel.getAttributeById(setting.getValue()) == null &&
                                    outputModel.getEntityById(setting.getValue()) == null) {
                                elementsExist = false;
                            }
                        } else {
                            HierarchicalModel outputModel = (HierarchicalModel) c.getOutputModel();
                            if (outputModel.getObjectById(setting.getModelObjectId()) == null) {
                                elementsExist = false;
                            }                        
                        }
                    }
                    if (!elementsExist) {
                        iter.remove();
                        context.getConfigurationService().delete(setting);
                    }
                }
            }
        }
    }
	
	@Override
	protected MappingDiagramState getState() {
		return (MappingDiagramState) super.getState();
	}

	public void removeSelected() {
		if (selectedSourceId != null) {
			removeConnection(selectedSourceId, selectedTargetId);
		}
	}

	public void filterInputModel(String text, boolean filterMapped) {
		callFunction("filterInputModel", text, filterMapped);
	}

	public void filterOutputModel(String text, boolean filterMapped) {
		callFunction("filterOutputModel", text, filterMapped);
	}

	protected void removeConnection(String sourceId, String targetId) {
		removeModelObjectConnection(sourceId, targetId);
		if (sourceId.equals(selectedSourceId) && targetId.equals(selectedTargetId)) {
			selectedSourceId = selectedTargetId = null;
			fireEvent(new SelectEvent(MappingDiagram.this, selectedSourceId, selectedTargetId));
		}			
	}
	
	protected void removeModelObjectConnection(String sourceId, String targetId) {
		List<ComponentModelSetting> settings = component.getModelSetting(sourceId, Mapping.MODEL_OBJECT_MAPS_TO);
		for (ComponentModelSetting setting : settings) {
			if (setting.getValue().equals(targetId)) {
				component.getModelSettings().remove(setting);
				context.getConfigurationService().delete(setting);
				markAsDirty();
			}
		}
	}
	
	protected void addConnection(String sourceId, String targetId) {
		Setting setting;
		setting = new ComponentModelSetting();
		ComponentModelSetting modelSetting = (ComponentModelSetting)setting;
		modelSetting.setComponentId(component.getId());
		modelSetting.setName(Mapping.MODEL_OBJECT_MAPS_TO);
		modelSetting.setModelObjectId(sourceId);
		modelSetting.setValue(targetId);
		modelSetting.setType(Type.ATTRIBUTE.toString());
		component.addModelSetting(modelSetting);		
		context.getConfigurationService().save(setting);			
		markAsDirty();
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
					addConnection(sourceId, targetId);
				}
			}
		}
	}
}
