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
package org.jumpmind.metl.core.persist;

import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentName;
import org.jumpmind.metl.core.model.EntityTag;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ModelRelation;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.ProjectVersionPlugin;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.Rppv;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.Tag;

public interface IConfigurationService {

    public List<ProjectVersionPlugin> findProjectVersionComponentPlugins(String projectVersionId);

    public void doInBackground();
    
    public void addConfigurationChangeListener(IConfigurationChangedListener listener);

    public FlowStep copy(FlowStep original);

    public void delete(Flow flow, FlowStep flowStep);

    public boolean delete(FlowStepLink link);

    public void delete(Folder folder);

    public void delete(Resource resource);

    public void deleteFlow(Flow flow);

    public List<Flow> findAffectedFlowsByFlow(String flowId);

    public List<Flow> findAffectedFlowsByModel(String modelId);

    public List<Flow> findAffectedFlowsByResource(String resourceId);
    
    public List<ProjectVersionDepends> findProjectDependenciesThatTarget(String projectVersionId);

    public List<ComponentName> findComponentsInProject(String projectVersionId);

    public void save(ProjectVersionPlugin projectVersionComponentPlugin);

    public void delete(AbstractObject obj);

    public void delete(Model model);

    public void delete(ModelEntity modelEntity);

    public void delete(ModelAttrib modelAttribute);

    public List<String> findAllProjectVersionIds();

    public List<Flow> findDependentFlows(String projectVersionId);

    public List<Model> findDependentModels(String flowId);

    public List<Resource> findDependentResources(String flowId);

    public List<Component> findDependentSharedComponents(String flowId);

    public Folder findFirstFolderWithName(String name, FolderType type);

    public Flow findFlow(String id);
    
    public FlowName findFlowName(String id);

    public List<FlowName> findFlows();
    
    public List<ResourceName> findResources();
    
    public List<ModelName> findModels();
    
    public List<ProjectVersionDepends> findProjectVersionDependencies();

    public List<Folder> findFolders(String projectVersionId, FolderType type);

    public List<FolderName> findFoldersInProject(String projectVersionId);

    public Model findModel(String id);
    
    public List<ProjectVersionDepends> findProjectDependencies(String projectVersionId);
    
    public List<FlowName> findFlowsInProject(String projectVersionId, boolean testFlows);

    public List<ModelName> findModelsInProject(String projectVersionId);
    
    public List<ResourceName> findResourcesInProject(String projectVersionId);

    public List<Project> findProjects();

    public ProjectVersion findProjectVersion(String projectVersionId);

    public Resource findResource(String id);    
    
    public List<Flow> findFlowsByName(String projectVersionId, String flowName);
    
    public List<Resource> findResourcesByName(String projectVersionId, String resourceName);

    public List<Model> findModelsByName(String projectVersionId, String modelName);

    public List<Resource> findResourcesByTypes(String projectVersionId, boolean includeDependencies, String... types);

    public Resource findPreviousVersionResource(Resource currentResource);

    public String getLastKnownVersion();

    public boolean isDeployed(Flow flow);

    public boolean isInstalled();

    public boolean isModelUsed(String id);

    public void refresh(Component component, boolean readRelations);

    public void refresh(Model model);

    public void refresh(Project project);

    public void refresh(ProjectVersion projectVersion);

    public void refresh(Resource resource);    

    public void save(AbstractObject obj);

    public void save(Component component);

    public void save(Flow flow);

    public void save(FlowStep flowStep);

    public void save(Model model);

    public void save(ModelEntity modelEntity);

    public void save(Project project);

    public void save(ProjectVersion projectVersion);

    public void save(Resource resource);

    public void save(Setting setting);
    
    public ProjectVersion saveNewVersion(String newVersionLabel, ProjectVersion original, String projectVersionType);

    public Flow copy(Flow original);

    public Flow copy(Map<String, AbstractObject> oldToNewUUIDMapping, Flow original, boolean newProjectVersion);

    public Model copy(Model original);
    
    public Model copy(Map<String, AbstractObject> oldToNewUUIDMapping, Model original);
    
    public Resource copy(Resource original);
    
    public Resource copy(Map<String, AbstractObject> oldToNewUUIDMapping, Resource original);

    Map<String, ProjectVersion> findProjectVersions();
    
    public List<ProjectVersion> findProjectVersionsByProject(Project project);
    
    public ReleasePackage findReleasePackage(String releasePackageId);
    
    public List<ReleasePackage> findReleasePackages();
    
    public void deleteReleasePackageProjectVersionsForReleasePackage(String releasePackageId);

    public List<Rppv> findReleasePackageProjectVersions(String releasePackageId);
    
    public void refresh(ReleasePackage releasePackage);
    
    public void updateProjectVersionDependency(ProjectVersionDepends dependency, String newTargetProjectVersionId);
    
    public void backupDatabase(String filePath);

    public void updateFlowWithNewGUIDs(String flowId,Map<String, String> oldToNewModelIdMap, 
            Map<String, String> oldToNewModelEntityIdMap, Map<String, String> oldToNewModelAttributeIdMap,
            Map<String, String> oldToNewResourceIdMap);
    
    public List<ModelRelation> findRelationshipsBetweenEntities(String entity1Id, String entity2Id);

    public List<Tag> findTags();

    public void refresh(Tag tag);

    public void delete(Tag tag);

    public List<EntityTag> findEntityTagsForEntity(String entityId);
    
    public void delete(EntityTag entityTag);
    
    public void deleteEntityTagsForTag(Tag tag);
    
    public void deleteEntityTags(String entityId);
    
    public List<Project> findProjectsWithTagLike(String tagFilterText);
    
    public Project findProject(String id);
    
    public List<Tag> findTagsForEntity(String entityType, String entityId);
}
