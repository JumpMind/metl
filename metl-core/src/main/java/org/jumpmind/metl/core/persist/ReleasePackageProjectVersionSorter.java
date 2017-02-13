package org.jumpmind.metl.core.persist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.ReleasePackageProjectVersion;

public class ReleasePackageProjectVersionSorter {
    
    IConfigurationService configurationService;

    public ReleasePackageProjectVersionSorter(IConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
    
    public List<ReleasePackageProjectVersion> sort(ReleasePackage releasePackage) {
        DirectedAcyclicGraph<String, DefaultEdge> dag = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);
        for (ReleasePackageProjectVersion rppv : releasePackage.getProjectVersions()) {
            dag.addVertex(rppv.getProjectVersionId());
            List<ProjectVersionDependency> dependencies = configurationService.findProjectDependencies(rppv.getProjectVersionId());
            for (ProjectVersionDependency dependency: dependencies) {
                dag.addVertex(dependency.getTargetProjectVersionId());
                dag.addEdge(dependency.getTargetProjectVersionId(), rppv.getProjectVersionId());
            }
        }      
        
        List<String> added = new ArrayList<>(releasePackage.getProjectVersions().size());
        List<ReleasePackageProjectVersion> list = new ArrayList<>(releasePackage.getProjectVersions().size());
        Iterator<String> itr = dag.iterator();
        while (itr.hasNext()) {                        
            String projectVersionId = itr.next();
            for (ReleasePackageProjectVersion rppv : releasePackage.getProjectVersions()) {
                if (rppv.getProjectVersionId().equals(projectVersionId) && !added.contains(rppv.getProjectVersionId())) {
                    list.add(rppv);
                    break;
                }
            }
        }
        return list;
    }

}
