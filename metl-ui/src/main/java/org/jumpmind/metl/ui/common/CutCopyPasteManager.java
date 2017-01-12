package org.jumpmind.metl.ui.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Notification.Type;

public class CutCopyPasteManager {

    public static final String CLIPBOARD_OBJECT_TYPE = "objectType";
    
    public static final String CLIPBOARD_ACTION = "action";
    
    public static final String CLIPBOARD_CUT = "cut";
    
    public static final String CLIPBOARD_COPY = "copy";
    
    public static final String CLIPBOARD_FLOW = "flow";
    
    public static final String CLIPBOARD_MODELS = "models";
    
    public static final String CLIPBOARD_RESOURCES = "resources";

    final Logger log = LoggerFactory.getLogger(getClass());
    ApplicationContext context;
    Map<String, Object> clipboard;
    IConfigurationService configurationService;
    Map<String, AbstractObject> oldToNewUUIDMapping;
        
    public CutCopyPasteManager(ApplicationContext context) {
        this.context = context;
        this.clipboard = context.getClipboard();
        this.configurationService = context.getConfigurationService();
        this.oldToNewUUIDMapping =  new HashMap<>();
    }
    
    public void cut(Object object) {
        //TODO:  Do something about flows in the deployed agents when things are cut or moved
        clipboard.clear();
        clipboard.put(CLIPBOARD_ACTION, CLIPBOARD_CUT);
        if (object instanceof FlowName) {
            Flow flow = configurationService.findFlow(((FlowName) object).getId());
            saveToClipboard(flow);
        } else if (object instanceof ModelName) {
            Model model = configurationService.findModel(((ModelName) object).getId());
            List<Flow> affectedFlows = configurationService.findAffectedFlowsByModel(model.getId());
            if (affectedFlows.size() > 0) {
                CommonUiUtils.notify("The model is currently in use.  It cannot be cut or moved.", Type.WARNING_MESSAGE);            
            } else {
                saveToClipboard(model);
            }
        } else if (object instanceof ResourceName) {
            Resource resource = configurationService.findResource(((ResourceName) object).getId());
            List<Flow> affectedFlows = configurationService.findAffectedFlowsByResource(resource.getId());
            if (affectedFlows.size() > 0) {
                CommonUiUtils.notify("The resource is currently in use.  It cannot be cut or moved.", Type.WARNING_MESSAGE);                            
            }
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
                && ((String) clipboard.get(CLIPBOARD_ACTION))
                        .equalsIgnoreCase(CLIPBOARD_CUT))
                && configurationService.findAffectedFlowsByFlow(flow.getId())
                        .size() == 0) {            
            configurationService.deleteFlow(flow);
        }        
    }
    
    public void pasteResources(String newProjectVersionId) {
        Map<String, AbstractObject> oldToNewUUIDMapping = new HashMap<>();
        pasteResources(oldToNewUUIDMapping, newProjectVersionId);
    }
    
    @SuppressWarnings("unchecked")
    protected void pasteResources(Map<String, AbstractObject> oldToNewUUIDMapping, String newProjectVersionId) {
        HashSet<Resource> origResources = (HashSet<Resource>) clipboard.get(CLIPBOARD_RESOURCES);
        HashSet<Resource> newResources = new HashSet<Resource>();
        for (Resource resource : origResources) {
            String existingResourceId = destinationHasResource(resource, newProjectVersionId);
            //make another copy if same project version.  If diff project version try and use existing
            if (existingResourceId == null || resource.getProjectVersionId() == newProjectVersionId) {
                //make a copy only if the resource is still in use by another flow
                //resource alone can't be cut if they have dependent flows
                if ((clipboard.containsKey(CLIPBOARD_ACTION) &&
                        ((String) clipboard.get(CLIPBOARD_ACTION)).equalsIgnoreCase(CLIPBOARD_COPY)) ||
                        configurationService.findAffectedFlowsByResource(resource.getId()).size() > 1) {
                    Resource newResource = configurationService.copy(oldToNewUUIDMapping, resource);
                    newResources.add(newResource);
                } else {
                    newResources.add(resource);
                }
            } else {
                Resource existingResource = configurationService.findResource(existingResourceId);
                mapResourceOldToNewUUID(oldToNewUUIDMapping, resource, existingResource);
            }
        }
        for (Resource resource : newResources) {
            resource.setProjectVersionId(newProjectVersionId);
            resource.setName(calculateResourceName(resource));           
            configurationService.save(resource);
        }  
    }
    
    private void mapResourceOldToNewUUID(Map<String, AbstractObject> oldToNewUUIDMapping, Resource oldResource, Resource newResource) {
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
    
    private String destinationHasResource(Resource resource, String newProjectVersionId) {
        List<Resource> existingResources = configurationService.findResourcesByName(newProjectVersionId, resource.getName());
        for (Resource existingResource : existingResources) {
            //findByName doesn't do deep fetch
            existingResource = configurationService.findResource(existingResource.getId());
            if (resourcesMatchAcrossProjects(resource, existingResource)) {
                return existingResource.getId();
            }
        }
        return null;
    }

    private String calculateFlowName(Flow flow) {
        //this has the new project version id and the old name
        String name = flow.getName();
        boolean calculatedName = false;
        int copyNumber = 1;
        do {
            List<Flow> existingFlows = configurationService.findFlowsByName(
                    flow.getProjectVersionId(), name);
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
        //this has the new project version id and the old name
        String name = resource.getName();
        boolean calculatedName = false;
        int copyNumber = 1;
        do {
            List<Resource> existingResources = configurationService.findResourcesByName(
                    resource.getProjectVersionId(), name);
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
        //this has the new project version id and the old name
        String name = model.getName();
        boolean calculatedName = false;
        int copyNumber = 1;
        do {
            List<Model> existingModels = configurationService.findModelsByName(
                    model.getProjectVersionId(), name);
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
                if (setting1.getName().equalsIgnoreCase(setting2.getName()) &&
                        setting1.getValue().equalsIgnoreCase(setting2.getValue())) {
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
    protected void pasteModels(Map<String, AbstractObject> oldToNewUUIDMapping, String newProjectVersionId) {       
        HashSet<Model> origModels = (HashSet<Model>) clipboard.get(CLIPBOARD_MODELS);
        HashSet<Model> newModels = new HashSet<Model>();
        for (Model model : origModels) {
            String existingModelId = destinationHasModel(model, newProjectVersionId);
            if (existingModelId == null || model.getProjectVersionId() == newProjectVersionId) {
                // make a copy only if the model is still in use by another flow
                // model alone can't be cut if they have dependent flows
                if ((clipboard.containsKey(CLIPBOARD_ACTION)
                        && ((String) clipboard.get(CLIPBOARD_ACTION))
                                .equalsIgnoreCase(CLIPBOARD_COPY))
                        || configurationService.findAffectedFlowsByModel(model.getId())
                                .size() > 1) {
                    Model newModel = configurationService.copy(oldToNewUUIDMapping, model);
                    newModels.add(newModel);
                } else {
                    newModels.add(model);
                }
            } else {
                Model existingModel = configurationService.findModel(existingModelId);
                mapModelOldToNewUUID(oldToNewUUIDMapping, model, existingModel);
            }
        }
        for (Model model : newModels) {
            model.setProjectVersionId(newProjectVersionId);
            model.setName(calculateModelName(model));
            configurationService.save(model);
        }        
    }
    
    private void mapModelOldToNewUUID(Map<String, AbstractObject> oldToNewUUIDMapping, Model oldModel, Model newModel) {
        oldToNewUUIDMapping.put(oldModel.getId(), newModel);
        for (ModelEntity oldEntity : oldModel.getModelEntities()) {
            ModelEntity newEntity = newModel.getEntityByName(oldEntity.getName());
            oldToNewUUIDMapping.put(oldEntity.getId(), newEntity);
            for (ModelAttribute oldAttribute : oldEntity.getModelAttributes()) {
                ModelAttribute newAttribute = newModel.getAttributeByName(oldEntity.getName(), oldAttribute.getName());
                oldToNewUUIDMapping.put(oldAttribute.getId(), newAttribute);
            }
        }
    }    
    
    private String destinationHasModel(Model model, String newProjectVersionId) {
        List<Model> existingModels = configurationService.findModelsByName(newProjectVersionId, model.getName());
        for (Model existingModel : existingModels) {
            //findByName doesn't do deep fetch
            existingModel = configurationService.findModel(existingModel.getId());
            if (modelsMatchAcrossProjects(model, existingModel)) {
                return existingModel.getId();
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
        List<ModelAttribute> attributes1 = entity1.getModelAttributes();
        List<ModelAttribute> attributes2 = entity2.getModelAttributes();
        for (ModelAttribute attribute1 : attributes1) {
            boolean foundMatch = false;
            for (ModelAttribute attribute2 : attributes2) {
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
        clipboard.put(CLIPBOARD_MODELS, new HashSet<Model>(configurationService.findDependentModels(flow.getId())));
        //TODO: update findDependentResources to look for resources in the settings as well as in the resource_id column of the component
        clipboard.put(CLIPBOARD_RESOURCES, new HashSet<Resource>(configurationService.findDependentResources(flow.getId())));   
    }
}
