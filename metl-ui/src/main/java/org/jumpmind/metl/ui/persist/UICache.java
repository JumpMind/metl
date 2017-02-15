package org.jumpmind.metl.ui.persist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AbstractObjectNameBasedSorter;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.persist.IConfigurationChangedListener;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UICache implements IUICache {

    static final Logger log = LoggerFactory.getLogger(UICache.class);

    IConfigurationService configurationService;
    
    IImportExportService importExportService;

    Map<String, List<ProjectVersionDependency>> dependenciesByProjectVersion;

    Map<String, List<FlowName>> flowsByProjectVersion;

    Map<String, List<ResourceName>> resourcesByProjectVersion;

    Map<String, List<ModelName>> modelsByProjectVersion;

    public UICache(IImportExportService importExportService, IConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.importExportService = importExportService;
    }
    
    public void init() {
        refreshAll();
        ChangeListener listener = new ChangeListener();
        this.configurationService.addConfigurationChangeListener(listener);
        this.importExportService.addConfigurationChangeListener(listener);
    }

    public List<ProjectVersionDependency> findProjectDependencies(String projectVersionId) {
        List<ProjectVersionDependency> list = dependenciesByProjectVersion.get(projectVersionId);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            dependenciesByProjectVersion.put(projectVersionId, list);
        }
        return list;
    }

    public List<FlowName> findFlowsInProject(String projectVersionId) {
        List<FlowName> list = flowsByProjectVersion.get(projectVersionId);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            flowsByProjectVersion.put(projectVersionId, list);
        }
        return list;

    }

    public List<ModelName> findModelsInProject(String projectVersionId) {
        List<ModelName> list = modelsByProjectVersion.get(projectVersionId);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            modelsByProjectVersion.put(projectVersionId, list);
        }
        return list;

    }

    public List<ResourceName> findResourcesInProject(String projectVersionId) {
        List<ResourceName> list=  resourcesByProjectVersion.get(projectVersionId);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            resourcesByProjectVersion.put(projectVersionId, list);
        }
        return list;

    }

    protected void refreshAll() {
        long ts = System.currentTimeMillis();
        List<FlowName> flows = configurationService.findFlows();
        Map<String, List<FlowName>> flowsByProjectVersionTemp = new HashMap<>();
        for (FlowName flowName : flows) {
            List<FlowName> flowNames = flowsByProjectVersionTemp.get(flowName.getProjectVersionId());
            if (flowNames == null) {
                flowNames = Collections.synchronizedList(new ArrayList<>());
                flowsByProjectVersionTemp.put(flowName.getProjectVersionId(), flowNames);
            }
            flowNames.add(flowName);
        }
        flowsByProjectVersion = Collections.synchronizedMap(flowsByProjectVersionTemp);

        List<ResourceName> resources = configurationService.findResources();
        Map<String, List<ResourceName>> resourcesByProjectVersionTemp = new HashMap<>();
        for (ResourceName obj : resources) {
            List<ResourceName> names = resourcesByProjectVersionTemp.get(obj.getProjectVersionId());
            if (names == null) {
                names = Collections.synchronizedList(new ArrayList<>());
                resourcesByProjectVersionTemp.put(obj.getProjectVersionId(), names);
            }
            names.add(obj);
        }
        resourcesByProjectVersion = Collections.synchronizedMap(resourcesByProjectVersionTemp);

        List<ModelName> models = configurationService.findModels();
        Map<String, List<ModelName>> modelsByProjectVersionTemp = new HashMap<>();
        for (ModelName obj : models) {
            List<ModelName> names = modelsByProjectVersionTemp.get(obj.getProjectVersionId());
            if (names == null) {
                names = Collections.synchronizedList(new ArrayList<>());
                modelsByProjectVersionTemp.put(obj.getProjectVersionId(), names);
            }
            names.add(obj);
        }
        modelsByProjectVersion = Collections.synchronizedMap(modelsByProjectVersionTemp);

        List<ProjectVersionDependency> dependencies = configurationService.findProjectVersionDependencies();
        Map<String, List<ProjectVersionDependency>> dependenciesByProjectVersionTemp = new HashMap<>();
        for (ProjectVersionDependency obj : dependencies) {
            List<ProjectVersionDependency> names = dependenciesByProjectVersionTemp.get(obj.getProjectVersionId());
            if (names == null) {
                names = Collections.synchronizedList(new ArrayList<>());
                dependenciesByProjectVersionTemp.put(obj.getProjectVersionId(), names);
            }
            names.add(obj);
        }
        dependenciesByProjectVersion = Collections.synchronizedMap(dependenciesByProjectVersionTemp);

        log.info("It took {}ms to refresh the ui cache", (System.currentTimeMillis() - ts));

    }

    class ChangeListener implements IConfigurationChangedListener {

        final protected AbstractObject convert(AbstractObject object) {
            if (object instanceof Flow) {
                object = new FlowName((Flow) object);
            } else if (object instanceof Resource) {
                object = new ResourceName((Resource) object);
            } else if (object instanceof Model) {
                object = new ModelName((Model) object);
            }
            return object;
        }

        @Override
        public void onMultiRowUpdate() {
            refreshAll();
        }

        @Override
        public void onDelete(AbstractObject object) {
            object = convert(object);
            if (object instanceof FlowName) {
                FlowName named = (FlowName) object;
                List<FlowName> names = flowsByProjectVersion.get(named.getProjectVersionId());
                if (names != null) {
                    names.remove(named);
                }
            } else if (object instanceof ResourceName) {
                ResourceName named = (ResourceName) object;
                List<ResourceName> names = resourcesByProjectVersion.get(named.getProjectVersionId());
                if (names != null) {
                    names.remove(named);
                }
            } else if (object instanceof ModelName) {
                ModelName named = (ModelName) object;
                List<ModelName> names = modelsByProjectVersion.get(named.getProjectVersionId());
                if (names != null) {
                    names.remove(named);
                }
            } else if (object instanceof ProjectVersionDependency) {
                ProjectVersionDependency named = (ProjectVersionDependency) object;
                List<ProjectVersionDependency> names = dependenciesByProjectVersion.get(named.getProjectVersionId());
                if (names != null) {
                    names.remove(named);
                }
            } else if (object instanceof ProjectVersion || object instanceof Project) {
                refreshAll();
            }
        }

        @Override
        public void onSave(AbstractObject object) {
            object = convert(object);
            if (object instanceof FlowName) {
                FlowName named = (FlowName) object;
                if (named.isDeleted()) {
                    onDelete(named);
                } else {
                    List<FlowName> names = flowsByProjectVersion.get(named.getProjectVersionId());
                    if (names != null) {
                        names.remove(named);
                        names.add(named);
                        AbstractObjectNameBasedSorter.sort(names);
                    }
                }
            } else if (object instanceof ResourceName) {
                ResourceName named = (ResourceName) object;
                if (named.isDeleted()) {
                    onDelete(named);
                } else {
                    List<ResourceName> names = resourcesByProjectVersion.get(named.getProjectVersionId());
                    if (names != null) {
                        names.remove(named);
                        names.add(named);
                        AbstractObjectNameBasedSorter.sort(names);
                    }
                }
            } else if (object instanceof ModelName) {
                ModelName named = (ModelName) object;
                if (named.isDeleted()) {
                    onDelete(named);
                } else {
                    List<ModelName> names = modelsByProjectVersion.get(named.getProjectVersionId());
                    if (names != null) {
                        names.remove(named);
                        names.add(named);
                        AbstractObjectNameBasedSorter.sort(names);
                    }
                }
            } else if (object instanceof ProjectVersionDependency) {
                ProjectVersionDependency named = (ProjectVersionDependency) object;
                List<ProjectVersionDependency> names = dependenciesByProjectVersion.get(named.getProjectVersionId());
                if (names != null) {
                    names.remove(named);
                    names.add(named);
                    AbstractObjectNameBasedSorter.sort(names);
                }
            } else if (object instanceof ProjectVersion || object instanceof Project) {
                refreshAll();
            }

        }

    }

}
