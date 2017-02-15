package org.jumpmind.metl.ui.persist;

import java.util.List;

import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.ResourceName;

public interface IUICache {
    
    public void init();

    public List<ProjectVersionDependency> findProjectDependencies(String projectVersionId);

    public List<FlowName> findFlowsInProject(String projectVersionId);

    public List<ModelName> findModelsInProject(String projectVersionId);

    public List<ResourceName> findResourcesInProject(String projectVersionId);

}
