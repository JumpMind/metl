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
package org.jumpmind.metl.ui.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CutCopyPasteManager {

    public static final String CLIPBOARD_OBJECT_TYPE = "objectType";

    public static final String CLIPBOARD_ACTION = "action";

    public static final String CLIPBOARD_CUT = "cut";

    public static final String CLIPBOARD_COPY = "copy";

    public static final String CLIPBOARD_FLOW = "flow";

    public static final String CLIPBOARD_MODELS = "models";

    public static final String CLIPBOARD_RESOURCES = "resources";
    
    public static final String CLIPBOARD_ORIGIN_FLOW = "flow";
    
    public static final String CLIPBOARD_ORIGIN_RESOURCE = "resource";
    
    public static final String CLIPBOARD_ORIGIN_MODEL = "model";

    final Logger log = LoggerFactory.getLogger(getClass());
    ApplicationContext context;
    Map<String, Object> clipboard;
    IConfigurationService configurationService;
    Map<String, AbstractObject> oldToNewUUIDMapping;

    public CutCopyPasteManager(ApplicationContext context) {
        this.context = context;
        this.clipboard = context.getClipboard();
        this.configurationService = context.getConfigurationService();
        this.oldToNewUUIDMapping = new HashMap<>();
    }

    public void cut(Object object) {
        // TODO: Do something about flows in the deployed agents when things are
        // cut or moved
        clipboard.clear();
        clipboard.put(CLIPBOARD_ACTION, CLIPBOARD_CUT);
        if (object instanceof FlowName) {
            Flow flow = configurationService.findFlow(((FlowName) object).getId());
            saveToClipboard(flow);
        } else if (object instanceof ModelName) {
            Model model = configurationService.findModel(((ModelName) object).getId());
            saveToClipboard(model);
        } else if (object instanceof ResourceName) {
            Resource resource = configurationService.findResource(((ResourceName) object).getId());
            saveToClipboard(resource);
        }
    }

    public void pasteFlow(String newProjectVersionId) {
        pasteResources(oldToNewUUIDMapping, newProjectVersionId);
        pasteModels(oldToNewUUIDMapping, newProjectVersionId);
        Flow flow = (Flow) clipboard.get(CLIPBOARD_FLOW);
        boolean newProjectVersion = flow.getProjectVersionId() != newProjectVersionId;
        Flow newFlow = configurationService.copy(oldToNewUUIDMapping, flow, newProjectVersion);
        newFlow.setProjectVersionId(newProjectVersionId);
        newFlow.setName(calculateFlowName(newFlow));
        configurationService.save(newFlow);
        if ((clipboard.containsKey(CLIPBOARD_ACTION)
                && ((String) clipboard.get(CLIPBOARD_ACTION)).equalsIgnoreCase(CLIPBOARD_CUT))
                && configurationService.findAffectedFlowsByFlow(flow.getId()).size() == 0) {
            configurationService.deleteFlow(flow);
        } else {
            newFlow.setRowId(UUID.randomUUID().toString());
            configurationService.save(newFlow);
        }
    }

    public void pasteResources(String newProjectVersionId) {
        Map<String, AbstractObject> oldToNewUUIDMapping = new HashMap<>();
        pasteResources(oldToNewUUIDMapping, newProjectVersionId);
    }

    @SuppressWarnings("unchecked")
    protected void pasteResources(Map<String, AbstractObject> oldToNewUUIDMapping,
            String newProjectVersionId) {
        
        HashSet<Resource> origResources = (HashSet<Resource>) clipboard.get(CLIPBOARD_RESOURCES);
        HashSet<Resource> newResources = new HashSet<Resource>();
            
        String cutCopyOrigin;
        if (clipboard.get(CLIPBOARD_FLOW) == null) {
            cutCopyOrigin = CLIPBOARD_ORIGIN_RESOURCE;
        } else {
            cutCopyOrigin = CLIPBOARD_ORIGIN_FLOW;
        }
        String action = (String) clipboard.get(CLIPBOARD_ACTION);
        
        Resource newResource;
        for (Resource resource : origResources) {
            int nbrAffectedFlows = configurationService.findAffectedFlowsByResource(resource.getId()).size();
            String existingResourceId = destinationHasResource(resource, newProjectVersionId, nbrAffectedFlows);
            boolean targetProjectHasResource = existingResourceId != null ? true : false;
            boolean targetProjectEqualsSourceProject = resource.getProjectVersionId().equalsIgnoreCase(newProjectVersionId) ? true : false;

            //make a duplicate copy of the resource if needed
            if (
                    // cut/copying a resource
                    (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_RESOURCE) &&
                    action.equalsIgnoreCase(CLIPBOARD_COPY) &&
                    (targetProjectEqualsSourceProject || (!targetProjectEqualsSourceProject || !targetProjectHasResource))) ||
                    // cut/copying a flow
                    (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_FLOW) &&
                    !targetProjectEqualsSourceProject &&
                    nbrAffectedFlows > 1 &&
                    !targetProjectHasResource)
                    //
                    ) {                    
                    newResource = configurationService.copy(oldToNewUUIDMapping, resource);
                    newResource.setProjectVersionId(newProjectVersionId);
                    newResource.setName(calculateResourceName(newResource));
            } else {
                newResource = resource;
            }
            
            if (targetProjectEqualsSourceProject || !targetProjectHasResource) {
                newResources.add(newResource);
            }

            //determine and restamp flows if needed
            if (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_RESOURCE) &&
                    action.equalsIgnoreCase(CLIPBOARD_CUT) &&
                    !targetProjectEqualsSourceProject &&
                    targetProjectHasResource) {                
                restampAllSourceProjectFlows(existingResourceId, newResource);
            } else if (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_FLOW) &&
                    !targetProjectEqualsSourceProject &&
                    ((action.equalsIgnoreCase(CLIPBOARD_CUT) && nbrAffectedFlows>1 && !targetProjectHasResource) ||
                            (action.equalsIgnoreCase(CLIPBOARD_CUT) && targetProjectHasResource) ||
                            (action.equalsIgnoreCase(CLIPBOARD_COPY) && targetProjectHasResource) ||
                            (action.equalsIgnoreCase(CLIPBOARD_COPY) && !targetProjectHasResource && nbrAffectedFlows > 1))) {
                Resource resourceToRestampTo;
                if (existingResourceId != null) {
                    resourceToRestampTo = configurationService.findResource(existingResourceId);
                } else {
                    resourceToRestampTo = newResource;
                }
                mapResourceOldToNewUUID(oldToNewUUIDMapping, resource, resourceToRestampTo);
            }

            //add project dependencies if needed
            if (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_RESOURCE) &&
                    action.equalsIgnoreCase(CLIPBOARD_CUT) &&
                    !targetProjectEqualsSourceProject &&
                    nbrAffectedFlows >= 1 &&
                    !dependencyExists(resource.getProjectVersionId(), newProjectVersionId)) {
                createNewProjectDependency(resource.getProjectVersionId(), newProjectVersionId);
            }
        }
        
        for (Resource resource : newResources) {
            resource.setProjectVersionId(newProjectVersionId);
            configurationService.save(resource);
        }
    }

    private boolean dependencyExists(String oldProjectVersionId, String newProjectVersionId) {
        List<ProjectVersionDepends> existingDependencies = configurationService.findProjectDependencies(oldProjectVersionId);
        for (ProjectVersionDepends dependency : existingDependencies) {
            if (dependency.getTargetProjectVersionId().equalsIgnoreCase(newProjectVersionId)) {
                return true;
            }
        }
        return false;
    }
    
    private void createNewProjectDependency(String oldProjectVersionId, String newProjectVersionId) {
        ProjectVersion newPvn = configurationService.findProjectVersion(newProjectVersionId);
        ProjectVersionDepends pvd = new ProjectVersionDepends();
        pvd.setProjectVersionId(oldProjectVersionId);
        pvd.setTargetProjectVersionId(newProjectVersionId);
        pvd.setRowId(UUID.randomUUID().toString());
        pvd.setName(String.format("%s (%s)", newPvn.getProject().getName(), newPvn.getVersionLabel()));
        pvd.setTargetProjectVersion(newPvn);
        configurationService.save(pvd);
    }
    
    private void restampAllSourceProjectFlows(String existingResourceId, Resource newResource) {
        Resource existingResource = configurationService.findResource(existingResourceId);
        mapResourceOldToNewUUID(oldToNewUUIDMapping, newResource, existingResource);
        Map<String, String> oldToNewUUIDStringMapping = new HashMap<String, String>();
        for (Map.Entry<String, AbstractObject> entry : oldToNewUUIDMapping.entrySet()) {
            oldToNewUUIDStringMapping.put(entry.getKey(), entry.getValue().getId());
        }
        List<Flow> affectedFlows = configurationService.findAffectedFlowsByResource(existingResource.getId());
        for (Flow flow : affectedFlows) {
            configurationService.updateFlowWithNewGUIDs(flow.getId(), null, null, null, oldToNewUUIDStringMapping);
        }
    }
    
    private void restampAllSourceProjectFlows(String existingModelId, Model newModel) {
        Model existingModel = configurationService.findModel(existingModelId);
        mapModelOldToNewUUID(oldToNewUUIDMapping, newModel, existingModel);
        Map<String, String> oldToNewUUIDStringMapping = new HashMap<String, String>();
        for (Map.Entry<String, AbstractObject> entry : oldToNewUUIDMapping.entrySet()) {
            oldToNewUUIDStringMapping.put(entry.getKey(), entry.getValue().getId());
        }
        List<Flow> affectedFlows = configurationService.findAffectedFlowsByModel(existingModel.getId());
        for (Flow flow : affectedFlows) {
            configurationService.updateFlowWithNewGUIDs(flow.getId(), oldToNewUUIDStringMapping, null, null, null);
        }
    }

    private void mapResourceOldToNewUUID(Map<String, AbstractObject> oldToNewUUIDMapping,
            Resource oldResource, Resource newResource) {
        oldToNewUUIDMapping.put(oldResource.getId(), newResource);

        for (Setting oldSetting : oldResource.getSettings()) {
            for (Setting newSetting : newResource.getSettings()) {
                if (oldSetting.getName().equalsIgnoreCase(newSetting.getName())) {
                    oldToNewUUIDMapping.put(oldSetting.getId(), newSetting);
                    break;
                }
            }
        }
    }

    private String destinationHasResource(Resource resource, String newProjectVersionId, int nbrDependentFlows) {
        List<String> projectVersionIds = new ArrayList<String>();
        projectVersionIds.add(newProjectVersionId);
        projectVersionIds.addAll(getDependentProjectVersionIds(newProjectVersionId));

        for (String projectVersionId : projectVersionIds) {
            List<Resource> existingResources = configurationService
                    .findResourcesByName(projectVersionId, resource.getName());
            for (Resource existingResource : existingResources) {
                // findByName doesn't do deep fetch
                existingResource = configurationService.findResource(existingResource.getId());
                if (resourcesMatchAcrossProjects(resource, existingResource) &&
                        !resourceInCutBuffer(existingResource, nbrDependentFlows)) {
                    return existingResource.getId();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean resourceInCutBuffer(Resource resource, int nbrDependentFlows) {
        if (clipboard.containsKey(CLIPBOARD_ACTION)
                && ((String) clipboard.get(CLIPBOARD_ACTION)).equalsIgnoreCase(CLIPBOARD_CUT)) {            
            HashSet<Resource> bufferResources = (HashSet<Resource>) clipboard.get(CLIPBOARD_RESOURCES);
            for (Resource bufferResource : bufferResources) {
                if (resource.getId().equalsIgnoreCase(bufferResource.getId()) &&
                        resource.getProjectVersionId().equalsIgnoreCase(bufferResource.getProjectVersionId()) &&
                        nbrDependentFlows <=1) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean modelInCutBuffer(Model model, int nbrDependentFlows) {
        if (clipboard.containsKey(CLIPBOARD_ACTION)
                && ((String) clipboard.get(CLIPBOARD_ACTION)).equalsIgnoreCase(CLIPBOARD_CUT)) {            
            HashSet<Model> bufferModels = (HashSet<Model>) clipboard.get(CLIPBOARD_MODELS);
            for (Model bufferModel : bufferModels) {
                if (model.getId().equalsIgnoreCase(bufferModel.getId()) &&
                        model.getProjectVersionId().equalsIgnoreCase(bufferModel.getProjectVersionId()) &&
                        nbrDependentFlows <=1) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> getDependentProjectVersionIds(String projectVersionId) {
        List<String> dependentProjectVersionIds = new ArrayList<String>();
        List<ProjectVersionDepends> projectDependencies = configurationService
                .findProjectDependencies(projectVersionId);
        for (ProjectVersionDepends projectDependency : projectDependencies) {
            dependentProjectVersionIds.add(projectDependency.getTargetProjectVersionId());
        }
        return dependentProjectVersionIds;
    }

    private String calculateFlowName(Flow flow) {
        // this has the new project version id and the old name
        String name = flow.getName();
        boolean calculatedName = false;
        int copyNumber = 1;
        do {
            List<Flow> existingFlows = configurationService
                    .findFlowsByName(flow.getProjectVersionId(), name);
            if (existingFlows.size() == 0) {
                calculatedName = true;
            } else {
                name = flow.getName() + " - " + String.valueOf(copyNumber);
                copyNumber++;
            }
        } while (!calculatedName);
        return name;
    }

    private String calculateResourceName(Resource resource) {
        // this has the new project version id and the old name
        String name = resource.getName();
        boolean calculatedName = false;
        int copyNumber = 1;
        do {
            List<Resource> existingResources = configurationService
                    .findResourcesByName(resource.getProjectVersionId(), name);
            if (existingResources.size() == 0) {
                calculatedName = true;
            } else {
                name = resource.getName() + " - " + String.valueOf(copyNumber);
                copyNumber++;
            }
        } while (!calculatedName);
        return name;
    }

    private String calculateModelName(Model model) {
        // this has the new project version id and the old name
        String name = model.getName();
        boolean calculatedName = false;
        int copyNumber = 1;
        do {
            List<Model> existingModels = configurationService
                    .findModelsByName(model.getProjectVersionId(), name);
            if (existingModels.size() == 0) {
                calculatedName = true;
            } else {
                name = model.getName() + " - " + String.valueOf(copyNumber);
                copyNumber++;
            }
        } while (!calculatedName);
        return name;
    }

    private boolean resourcesMatchAcrossProjects(Resource resource1, Resource resource2) {
        boolean matches = true;
        List<Setting> settings1 = resource1.getSettings();
        List<Setting> settings2 = resource2.getSettings();
        for (Setting setting1 : settings1) {
            boolean foundMatch = false;
            for (Setting setting2 : settings2) {
                if (setting1.getName().equalsIgnoreCase(setting2.getName())
                        && ((setting1.getValue() == null && setting2.getValue() == null) ||
                        setting1.getValue().equalsIgnoreCase(setting2.getValue()))) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    public void pasteModels(String newProjectVersionId) {
        Map<String, AbstractObject> oldToNewUUIDMapping = new HashMap<>();
        pasteModels(oldToNewUUIDMapping, newProjectVersionId);
    }

    @SuppressWarnings("unchecked")
    protected void pasteModels(Map<String, AbstractObject> oldToNewUUIDMapping,
            String newProjectVersionId) {

        HashSet<Model> origModels = (HashSet<Model>) clipboard.get(CLIPBOARD_MODELS);
        HashSet<Model> newModels = new HashSet<Model>();
            
        String cutCopyOrigin;
        if (clipboard.get(CLIPBOARD_FLOW) == null) {
            cutCopyOrigin = CLIPBOARD_ORIGIN_MODEL;
        } else {
            cutCopyOrigin = CLIPBOARD_ORIGIN_FLOW;
        }
        String action = (String) clipboard.get(CLIPBOARD_ACTION);
        
        Model newModel;
        for (Model model : origModels) {
            int nbrAffectedFlows = configurationService.findAffectedFlowsByModel(model.getId()).size();
            String existingModelId = destinationHasModel(model, newProjectVersionId, nbrAffectedFlows);
            boolean targetProjectHasModel = existingModelId != null ? true : false;
            boolean targetProjectEqualsSourceProject = model.getProjectVersionId().equalsIgnoreCase(newProjectVersionId) ? true : false;

            //make a duplicate copy of the model if needed
            if (
                    // cut/copying a resource
                    (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_MODEL) &&
                    action.equalsIgnoreCase(CLIPBOARD_COPY) &&
                    (targetProjectEqualsSourceProject || (!targetProjectEqualsSourceProject || !targetProjectHasModel))) ||
                    // cut/copying a flow
                    (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_FLOW) &&
                    !targetProjectEqualsSourceProject &&
                    nbrAffectedFlows > 1 &&
                    !targetProjectHasModel)
                    //
                    ) {                    
                    newModel = configurationService.copy(oldToNewUUIDMapping, model);
                    newModel.setProjectVersionId(newProjectVersionId);
                    newModel.setName(calculateModelName(newModel));
            } else {
                newModel = model;
            }
            
            if (targetProjectEqualsSourceProject || !targetProjectHasModel) {
                newModels.add(newModel);
            }

            //determine and restamp flows if needed
            if (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_MODEL) &&
                    action.equalsIgnoreCase(CLIPBOARD_CUT) &&
                    !targetProjectEqualsSourceProject &&
                    targetProjectHasModel) {                
                restampAllSourceProjectFlows(existingModelId, newModel);
            } else if (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_FLOW) &&
                    !targetProjectEqualsSourceProject &&
                    ((action.equalsIgnoreCase(CLIPBOARD_CUT) && nbrAffectedFlows>1 && !targetProjectHasModel) ||
                            (action.equalsIgnoreCase(CLIPBOARD_CUT) && targetProjectHasModel) ||
                            (action.equalsIgnoreCase(CLIPBOARD_COPY) && targetProjectHasModel) ||
                            (action.equalsIgnoreCase(CLIPBOARD_COPY) && !targetProjectHasModel && nbrAffectedFlows > 1))) {
                Model modelToRestampTo;
                if (existingModelId != null) {
                    modelToRestampTo = configurationService.findModel(existingModelId);
                } else {
                    modelToRestampTo = newModel;
                }                
                mapModelOldToNewUUID(oldToNewUUIDMapping, model, modelToRestampTo);
            }           
            
            //add project dependencies if needed
            if (cutCopyOrigin.equalsIgnoreCase(CLIPBOARD_ORIGIN_MODEL) &&
                    action.equalsIgnoreCase(CLIPBOARD_CUT) &&
                    !targetProjectEqualsSourceProject &&
                    nbrAffectedFlows >= 1 &&
                    !dependencyExists(model.getProjectVersionId(), newProjectVersionId)) {
                createNewProjectDependency(model.getProjectVersionId(), newProjectVersionId);
            }
        }
        
        for (Model model : newModels) {
            model.setProjectVersionId(newProjectVersionId);
            configurationService.save(model);
        }
    }

    private void mapModelOldToNewUUID(Map<String, AbstractObject> oldToNewUUIDMapping,
            Model oldModel, Model newModel) {
        oldToNewUUIDMapping.put(oldModel.getId(), newModel);
        for (ModelEntity oldEntity : oldModel.getModelEntities()) {
            ModelEntity newEntity = newModel.getEntityByName(oldEntity.getName());
            oldToNewUUIDMapping.put(oldEntity.getId(), newEntity);
            for (ModelAttrib oldAttribute : oldEntity.getModelAttributes()) {
                ModelAttrib newAttribute = newModel.getAttributeByName(oldEntity.getName(),
                        oldAttribute.getName());
                oldToNewUUIDMapping.put(oldAttribute.getId(), newAttribute);
            }
        }
    }

    private String destinationHasModel(Model model, String newProjectVersionId, int nbrAffectedFlows) {

        List<String> projectVersionIds = new ArrayList<String>();
        projectVersionIds.add(newProjectVersionId);
        projectVersionIds.addAll(getDependentProjectVersionIds(newProjectVersionId));

        for (String projectVersionId : projectVersionIds) {
            List<Model> existingModels = configurationService.findModelsByName(projectVersionId,
                    model.getName());
            for (Model existingModel : existingModels) {
                // findByName doesn't do deep fetch
                existingModel = configurationService.findModel(existingModel.getId());
                if (modelsMatchAcrossProjects(model, existingModel) &&
                        !modelInCutBuffer(existingModel, nbrAffectedFlows)) {
                    return existingModel.getId();
                }
            }
        }
        return null;
    }

    private boolean modelsMatchAcrossProjects(Model model1, Model model2) {
        boolean matches = true;
        List<ModelEntity> entities1 = model1.getModelEntities();
        List<ModelEntity> entities2 = model2.getModelEntities();
        for (ModelEntity entity1 : entities1) {
            boolean foundMatch = false;
            for (ModelEntity entity2 : entities2) {
                if (entity1.getName().equalsIgnoreCase(entity2.getName())) {
                    if (modelAttributesMatchAcrossEntities(entity1, entity2)) {
                        foundMatch = true;
                        break;
                    }
                }
            }
            if (!foundMatch) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    private boolean modelAttributesMatchAcrossEntities(ModelEntity entity1, ModelEntity entity2) {
        boolean matches = true;
        List<ModelAttrib> attributes1 = entity1.getModelAttributes();
        List<ModelAttrib> attributes2 = entity2.getModelAttributes();
        for (ModelAttrib attribute1 : attributes1) {
            boolean foundMatch = false;
            for (ModelAttrib attribute2 : attributes2) {
                if (attribute1.getName().equalsIgnoreCase(attribute2.getName())) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    public void copy(Object object) {
        clipboard.clear();
        clipboard.put(CLIPBOARD_ACTION, CLIPBOARD_COPY);
        oldToNewUUIDMapping.clear();
        if (object instanceof ModelName) {
            Model model = configurationService.findModel(((ModelName) object).getId());
            saveToClipboard(model);
        } else if (object instanceof ResourceName) {
            Resource resource = configurationService.findResource(((ResourceName) object).getId());
            saveToClipboard(resource);
        } else if (object instanceof FlowName) {
            Flow flow = configurationService.findFlow(((FlowName) object).getId());
            saveToClipboard(flow);
        }
    }

    private void saveToClipboard(Model model) {
        clipboard.put(CLIPBOARD_OBJECT_TYPE, Model.class);
        HashSet<Model> models = new HashSet<Model>();
        models.add(model);
        clipboard.put(CLIPBOARD_MODELS, models);
    }

    private void saveToClipboard(Resource resource) {
        clipboard.put(CLIPBOARD_OBJECT_TYPE, Resource.class);
        HashSet<Resource> resources = new HashSet<Resource>();
        resources.add(resource);
        clipboard.put(CLIPBOARD_RESOURCES, resources);
    }

    private void saveToClipboard(Flow flow) {
        clipboard.put(CLIPBOARD_OBJECT_TYPE, Flow.class);
        clipboard.put(CLIPBOARD_FLOW, flow);
        clipboard.put(CLIPBOARD_MODELS,
                new HashSet<Model>(configurationService.findDependentModels(flow.getId())));
        clipboard.put(CLIPBOARD_RESOURCES,
                new HashSet<Resource>(configurationService.findDependentResources(flow.getId())));
    }
}
