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
import org.jumpmind.metl.core.model.HierarchicalModel;
import org.jumpmind.metl.core.model.HierarchicalModelName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.RelationalModelName;
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

    Map<String, List<ProjectVersionDepends>> dependenciesByProjectVersion;

    Map<String, String> projectVersionsById;

    Map<String, List<FlowName>> flowsByProjectVersion;
    
    Map<String, List<ResourceName>> resourcesByProjectVersion;
    
    Map<String, List<RelationalModelName>> relationalModelsByProjectVersion;
    
    Map<String, List<HierarchicalModelName>> hierarchicalModelsByProjectVersion;

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

    public List<ProjectVersionDepends> findProjectDependencies(String projectVersionId) {
        List<ProjectVersionDepends> list = dependenciesByProjectVersion.get(projectVersionId);
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

    public List<RelationalModelName> findRelationalModelsInProject(String projectVersionId) {
        List<RelationalModelName> list = relationalModelsByProjectVersion.get(projectVersionId);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            relationalModelsByProjectVersion.put(projectVersionId, list);
        }
        return list;

    }

    public List<HierarchicalModelName> findHierarchicalModelsInProject(String projectVersionId) {
        List<HierarchicalModelName> list = hierarchicalModelsByProjectVersion.get(projectVersionId);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<>());
            hierarchicalModelsByProjectVersion.put(projectVersionId, list);
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
        Map<String, String> projectVersionsByIdTemp = new HashMap<>();
        List<FlowName> flows = configurationService.findFlows();
        Map<String, List<FlowName>> flowsByProjectVersionTemp = new HashMap<>();
        for (FlowName flowName : flows) {
            projectVersionsByIdTemp.put(flowName.getId(), flowName.getProjectVersionId());
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
            projectVersionsByIdTemp.put(obj.getId(), obj.getProjectVersionId());
            List<ResourceName> names = resourcesByProjectVersionTemp.get(obj.getProjectVersionId());
            if (names == null) {
                names = Collections.synchronizedList(new ArrayList<>());
                resourcesByProjectVersionTemp.put(obj.getProjectVersionId(), names);
            }
            names.add(obj);
        }
        resourcesByProjectVersion = Collections.synchronizedMap(resourcesByProjectVersionTemp);

        List<RelationalModelName> relationalModels = configurationService.findRelationalModels();
        Map<String, List<RelationalModelName>> relationalModelsByProjectVersionTemp = new HashMap<>();
        for (RelationalModelName obj : relationalModels) {
            projectVersionsByIdTemp.put(obj.getId(), obj.getProjectVersionId());
            List<RelationalModelName> names = relationalModelsByProjectVersionTemp.get(obj.getProjectVersionId());
            if (names == null) {
                names = Collections.synchronizedList(new ArrayList<>());
                relationalModelsByProjectVersionTemp.put(obj.getProjectVersionId(), names);
            }
            names.add(obj);
        }
        relationalModelsByProjectVersion = Collections.synchronizedMap(relationalModelsByProjectVersionTemp);

        List<HierarchicalModelName> hierarchicalModels = configurationService.findHierarchicalModels();
        Map<String, List<HierarchicalModelName>> hierarchicalModelsByProjectVersionTemp = new HashMap<>();
        for (HierarchicalModelName obj : hierarchicalModels) {
            projectVersionsByIdTemp.put(obj.getId(), obj.getProjectVersionId());
            List<HierarchicalModelName> names = hierarchicalModelsByProjectVersionTemp.get(obj.getProjectVersionId());
            if (names == null) {
                names = Collections.synchronizedList(new ArrayList<>());
                hierarchicalModelsByProjectVersionTemp.put(obj.getProjectVersionId(), names);
            }
            names.add(obj);
        }
        hierarchicalModelsByProjectVersion = Collections.synchronizedMap(hierarchicalModelsByProjectVersionTemp);

        List<ProjectVersionDepends> dependencies = configurationService.findProjectVersionDependencies();
        Map<String, List<ProjectVersionDepends>> dependenciesByProjectVersionTemp = new HashMap<>();
        for (ProjectVersionDepends obj : dependencies) {
            List<ProjectVersionDepends> names = dependenciesByProjectVersionTemp.get(obj.getProjectVersionId());
            if (names == null) {
                names = Collections.synchronizedList(new ArrayList<>());
                dependenciesByProjectVersionTemp.put(obj.getProjectVersionId(), names);
            }
            names.add(obj);
        }
        dependenciesByProjectVersion = Collections.synchronizedMap(dependenciesByProjectVersionTemp);

        projectVersionsById = Collections.synchronizedMap(projectVersionsByIdTemp);
        
        log.info("It took {}ms to refresh the ui cache", (System.currentTimeMillis() - ts));
    }

    class ChangeListener implements IConfigurationChangedListener {

        final protected AbstractObject convert(AbstractObject object) {
            if (object instanceof Flow) {
                object = new FlowName((Flow) object);
            } else if (object instanceof Resource) {
                object = new ResourceName((Resource) object);
            } else if (object instanceof RelationalModel) {
                object = new RelationalModelName((RelationalModel) object);
            } else if (object instanceof HierarchicalModel) {
                object = new HierarchicalModelName((HierarchicalModel) object);
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
            projectVersionsById.remove(object.getId());
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
            } else if (object instanceof RelationalModelName) {
                RelationalModelName named = (RelationalModelName) object;
                List<RelationalModelName> names = relationalModelsByProjectVersion.get(named.getProjectVersionId());
                if (names != null) {
                    names.remove(named);
                }
            } else if (object instanceof HierarchicalModelName) {
                HierarchicalModelName named = (HierarchicalModelName) object;
                List<HierarchicalModelName> names = hierarchicalModelsByProjectVersion.get(named.getProjectVersionId());
                if (names != null) {
                    names.remove(named);
                }
            } else if (object instanceof ProjectVersionDepends) {
                ProjectVersionDepends named = (ProjectVersionDepends) object;
                List<ProjectVersionDepends> names = dependenciesByProjectVersion.get(named.getProjectVersionId());
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
            String oldProjectVersionId = projectVersionsById.get(object.getId());
            if (object instanceof FlowName) {                
                FlowName named = (FlowName) object;
                if (named.isDeleted()) {
                    onDelete(named);
                } else {
                    projectVersionsById.put(named.getId(), named.getProjectVersionId());
                    List<FlowName> names = flowsByProjectVersion.get(named.getProjectVersionId());                    
                    if (names != null) {
                        removeOldFlowPvEntryIfNeeded(oldProjectVersionId, named.getProjectVersionId(), named.getId());
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
                    projectVersionsById.put(named.getId(), named.getProjectVersionId());                    
                    List<ResourceName> names = resourcesByProjectVersion.get(named.getProjectVersionId());
                    if (names != null) {
                        removeOldResourcePvEntryIfNeeded(oldProjectVersionId, named.getProjectVersionId(), named.getId());
                        names.remove(named);
                        names.add(named);
                        AbstractObjectNameBasedSorter.sort(names);
                    }
                }
            } else if (object instanceof RelationalModelName) {
                RelationalModelName named = (RelationalModelName) object;
                if (named.isDeleted()) {
                    onDelete(named);
                } else {
                    projectVersionsById.put(named.getId(), named.getProjectVersionId());
                    List<RelationalModelName> names = relationalModelsByProjectVersion.get(named.getProjectVersionId());
                    if (names != null) {
                        removeOldModelPvEntryIfNeeded(oldProjectVersionId, named.getProjectVersionId(), named.getId());
                        names.remove(named);
                        names.add(named);
                        AbstractObjectNameBasedSorter.sort(names);
                    }
                }
            } else if (object instanceof HierarchicalModelName) {
                HierarchicalModelName named = (HierarchicalModelName) object;
                if (named.isDeleted()) {
                    onDelete(named);
                } else {
                    projectVersionsById.put(named.getId(), named.getProjectVersionId());
                    List<HierarchicalModelName> names = hierarchicalModelsByProjectVersion.get(named.getProjectVersionId());
                    if (names != null) {
                        removeOldModelPvEntryIfNeeded(oldProjectVersionId, named.getProjectVersionId(), named.getId());
                        names.remove(named);
                        names.add(named);
                        AbstractObjectNameBasedSorter.sort(names);
                    }
                }
            } else if (object instanceof ProjectVersionDepends) {
                ProjectVersionDepends named = (ProjectVersionDepends) object;
                List<ProjectVersionDepends> names = dependenciesByProjectVersion.get(named.getProjectVersionId());
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
    protected void removeOldFlowPvEntryIfNeeded(String oldProjectVersionId, String newProjectVersionId, String id) {
        
        if (oldProjectVersionId != null && !oldProjectVersionId.equalsIgnoreCase(newProjectVersionId)) {
            List<FlowName> oldPvNames = flowsByProjectVersion.get(oldProjectVersionId);
            FlowName objectToRemove=null;            
            for (FlowName oldPvName: oldPvNames) {
                if (oldPvName.getId().equalsIgnoreCase(id)) {
                    objectToRemove = oldPvName;
                    break;
                }
            }
            if (objectToRemove != null) {
                oldPvNames.remove(objectToRemove);
            }
        }
    }
    
    protected void removeOldModelPvEntryIfNeeded(String oldProjectVersionId, String newProjectVersionId, String id) {
        
        if (oldProjectVersionId != null && !oldProjectVersionId.equalsIgnoreCase(newProjectVersionId)) {
            List<RelationalModelName> oldPvNames = relationalModelsByProjectVersion.get(oldProjectVersionId);
            RelationalModelName objectToRemove=null;
            for (RelationalModelName oldPvName: oldPvNames) {
                if (oldPvName.getId().equalsIgnoreCase(id)) {
                    objectToRemove = oldPvName;
                    break;
                }
            }
           if (objectToRemove != null) {
               oldPvNames.remove(objectToRemove);
           }
        }
    }

    protected void removeOldResourcePvEntryIfNeeded(String oldProjectVersionId, String newProjectVersionId, String id) {
        
        if (oldProjectVersionId != null && !oldProjectVersionId.equalsIgnoreCase(newProjectVersionId)) {
            List<ResourceName> oldPvNames = resourcesByProjectVersion.get(oldProjectVersionId);
            ResourceName objectToRemove=null;            
            for (ResourceName oldPvName: oldPvNames) {
                if (oldPvName.getId().equalsIgnoreCase(id)) {
                    objectToRemove = oldPvName;
                    break;
                }
            }
            if (objectToRemove != null) {
                oldPvNames.remove(objectToRemove);
            }
        }
    }
}
