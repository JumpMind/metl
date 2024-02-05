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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Page;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

@CssImport("./mapping-diagram.css")
@JsModule("./mapping-diagram.js")
@JsModule("jsplumb")
@JavaScript("./mapping-diagram.js")
@SuppressWarnings("serial")
public class MappingDiagram extends Div {
    
    private MappingDiagramDetail diagramDetail;
    
    private IMappingPanel panel;

	ApplicationContext context;

	Component component;

	String selectedSourceId;

	String selectedTargetId;

	public MappingDiagram(ApplicationContext context, Component component, IMappingPanel panel) {
		this.context = context;
		this.component = component;
		this.panel = panel;
		diagramDetail = new MappingDiagramDetail();
		addClassName("mapping-diagram");
		setId("mapping-diagram");

		cleanAbandonedLinks(component);

		diagramDetail.setComponent(component);

        if (component.getInputModel() instanceof RelationalModel) {
            diagramDetail.setRelationalInputModel((RelationalModel) component.getInputModel());
            context.getConfigurationService().refresh(diagramDetail.getRelationalInputModel());
            Collections.sort((diagramDetail.getRelationalInputModel()).getModelEntities(), new ModelEntitySorter());
            diagramDetail.getRelationalInputModel().sortAttributes();
        } else {
            diagramDetail.setHierarchicalInputModel((HierarchicalModel) component.getInputModel());
            context.getConfigurationService().refresh(diagramDetail.getHierarchicalInputModel());
        }

        if (component.getOutputModel() instanceof RelationalModel) {
            diagramDetail.setRelationalOutputModel((RelationalModel) component.getOutputModel());
            if (diagramDetail.getRelationalOutputModel() != null) {
                context.getConfigurationService().refresh(diagramDetail.getRelationalOutputModel());
                Collections.sort((diagramDetail.getRelationalOutputModel()).getModelEntities(), new ModelEntitySorter());
                diagramDetail.getRelationalOutputModel().sortAttributes();
            }
        } else {
            diagramDetail.setHierarchicalOutputModel((HierarchicalModel) component.getOutputModel());
            context.getConfigurationService().refresh(diagramDetail.getHierarchicalOutputModel());            
        }		
	}
	
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Page page = UI.getCurrent().getPage();
        page.executeJs("window.org_jumpmind_metl_ui_mapping_MappingDiagram()");
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

	public void removeSelected() {
		if (selectedSourceId != null) {
			removeConnection(selectedSourceId, selectedTargetId);
		}
	}

	public void filterInputModel(String text, boolean filterMapped) {
		UI.getCurrent().getPage().executeJs("filterInputModel($0,$1)", text, filterMapped);
	}

	public void filterOutputModel(String text, boolean filterMapped) {
		UI.getCurrent().getPage().executeJs("filterOutputModel($0,$1)", text, filterMapped);
	}

	protected void removeConnection(String sourceId, String targetId) {
		removeModelObjectConnection(sourceId, targetId);
		if (sourceId.equals(selectedSourceId) && targetId.equals(selectedTargetId)) {
			selectedSourceId = selectedTargetId = null;
			panel.selectEvent(new SelectEvent(MappingDiagram.this, selectedSourceId, selectedTargetId));
		}			
	}
	
	protected void removeModelObjectConnection(String sourceId, String targetId) {
		List<ComponentModelSetting> settings = component.getModelSetting(sourceId, Mapping.MODEL_OBJECT_MAPS_TO);
		for (ComponentModelSetting setting : settings) {
			if (setting.getValue().equals(targetId)) {
				component.getModelSettings().remove(setting);
				context.getConfigurationService().delete(setting);
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
	}	
	
    @ClientCallable
    private void onSelect(JsonObject json) {
        selectedSourceId = json.getString("sourceId").substring(3);
        selectedTargetId = json.getString("targetId").substring(3);
        panel.selectEvent(new SelectEvent(MappingDiagram.this, selectedSourceId, selectedTargetId));
    }
    
    @ClientCallable
    private void onConnection(JsonObject json) {
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
