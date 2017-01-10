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
        Flow newFlow = configurationService.copy(flow);
        newFlow.setProjectVersionId(newProjectVersionId);
        configurationService.save(newFlow);
        if ((clipboard.containsKey(CLIPBOARD_ACTION)
                && ((String) clipboard.get(CLIPBOARD_ACTION))
                        .equalsIgnoreCase(CLIPBOARD_CUT))
                && configurationService.findAffectedFlowsByFlow(flow.getId())
                        .size() == 0) {            
            configurationService.delete(flow);
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
            if (!destinationHasResource(resource, newProjectVersionId)) {
                //make a copy only if the resource is still in use by another flow
                //resource alone can't be cut if they have dependent flows
                if ((clipboard.containsKey(CLIPBOARD_ACTION) &&
                        ((String) clipboard.get(CLIPBOARD_ACTION)).equalsIgnoreCase(CLIPBOARD_COPY)) ||
                        configurationService.findAffectedFlowsByResource(resource.getId()).size() > 1) {
                    Resource newResource = configurationService.copy(resource);
                    newResources.add(newResource);
                    oldToNewUUIDMapping.put(resource.getId(), newResource);
                } else {
                    newResources.add(resource);
                }
            }
        }
        for (Resource resource : newResources) {
            resource.setProjectVersionId(newProjectVersionId);
            resource.setName(calculateResourceName(resource));           
            configurationService.save(resource);
        }  
    }
    
    private boolean destinationHasResource(Resource resource, String newProjectVersionId) {
        boolean destinationHasResource = false;
        List<Resource> existingResources = configurationService.findResourcesByName(newProjectVersionId, resource.getName());
        for (Resource existingResource : existingResources) {
            //findByName doesn't do deep fetch
            existingResource = configurationService.findResource(existingResource.getId());
            if (resourcesMatchAcrossProjects(resource, existingResource)) {
                destinationHasResource = true;
                break;
            }
        }
        return destinationHasResource;
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
            if (!destinationHasModel(model, newProjectVersionId)) {
                // make a copy only if the model is still in use by another flow
                // model alone can't be cut if they have dependent flows
                if ((clipboard.containsKey(CLIPBOARD_ACTION)
                        && ((String) clipboard.get(CLIPBOARD_ACTION))
                                .equalsIgnoreCase(CLIPBOARD_COPY))
                        || configurationService.findAffectedFlowsByModel(model.getId())
                                .size() > 1) {
                    Model newModel = configurationService.copy(model);
                    newModels.add(newModel);
                    oldToNewUUIDMapping.put(model.getId(), newModel);
                } else {
                    newModels.add(model);
                }
            }
        }
        for (Model model : newModels) {
            model.setProjectVersionId(newProjectVersionId);
            model.setName(calculateModelName(model));
            configurationService.save(model);
        }        
    }
    
    private boolean destinationHasModel(Model model, String newProjectVersionId) {
        boolean destinationHasModel = false;
        List<Model> existingModels = configurationService.findModelsByName(newProjectVersionId, model.getName());
        for (Model existingModel : existingModels) {
            //findByName doesn't do deep fetch
            existingModel = configurationService.findModel(existingModel.getId());
            if (modelsMatchAcrossProjects(model, existingModel)) {
                destinationHasModel = true;
                break;
            }
        }
        return destinationHasModel;
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
