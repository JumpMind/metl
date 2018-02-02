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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.Rppv;

public class ReleasePackageProjectVersionSorter {
    
    IConfigurationService configurationService;

    public ReleasePackageProjectVersionSorter(IConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
    
    public List<Rppv> sort(ReleasePackage releasePackage) {
        DirectedAcyclicGraph<String, DefaultEdge> dag = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);
        for (Rppv rppv : releasePackage.getProjectVersions()) {
            dag.addVertex(rppv.getProjectVersionId());
            List<ProjectVersionDepends> dependencies = configurationService.findProjectDependencies(rppv.getProjectVersionId());
            for (ProjectVersionDepends dependency: dependencies) {
                dag.addVertex(dependency.getTargetProjectVersionId());
                dag.addEdge(dependency.getTargetProjectVersionId(), rppv.getProjectVersionId());
            }
        }      
        
        List<String> added = new ArrayList<>(releasePackage.getProjectVersions().size());
        List<Rppv> list = new ArrayList<>(releasePackage.getProjectVersions().size());
        Iterator<String> itr = dag.iterator();
        while (itr.hasNext()) {                        
            String projectVersionId = itr.next();
            for (Rppv rppv : releasePackage.getProjectVersions()) {
                if (rppv.getProjectVersionId().equals(projectVersionId) && !added.contains(rppv.getProjectVersionId())) {
                    list.add(rppv);
                    break;
                }
            }
        }
        return list;
    }

}
