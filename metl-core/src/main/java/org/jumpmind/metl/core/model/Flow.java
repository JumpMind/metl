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
package org.jumpmind.metl.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.jumpmind.metl.core.runtime.component.ComponentTypeIdConstants.*;

public class Flow extends AbstractNamedObject implements IAuditable {

    private static final long serialVersionUID = 1L;

    String rowId = UUID.randomUUID().toString();

    Folder folder;

    String projectVersionId;

    String name;
    
    String notes;

    List<FlowStep> flowSteps;

    List<FlowStepLink> flowStepLinks;

    List<FlowParameter> flowParameters;

    boolean deleted = false;
    
    boolean test = false;
    
    public Flow() {
        this.flowSteps = new ArrayList<FlowStep>();
        this.flowStepLinks = new ArrayList<FlowStepLink>();
        this.flowParameters = new ArrayList<FlowParameter>();
    }

    public Flow(Folder folder) {
        this();
        setFolder(folder);
    }

    public Flow(String id) {
        this();
        setId(id);
    }
    
    public void setWebService(boolean webService) {
    }
    
    public boolean isWebService() {
        for (FlowStep flowStep : flowSteps) {
            if (HTTP_REQUEST.equals(flowStep.getComponent().getType())) {
                return true;
            }
        }
        return false;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setFolderId(String folderId) {
        if (folderId != null) {
            this.folder = new Folder(folderId);
        } else {
            this.folder = null;
        }
    }

    public String getFolderId() {
        return folder != null ? folder.getId() : null;
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    public Set<Resource> findResources() {
        HashSet<Resource> resources = new HashSet<Resource>();
        for (FlowStep flowStep : flowSteps) {
            Resource resource = flowStep.getComponent().getResource();
            if (resource != null) {
                resources.add(resource);
            }
        }
        return resources;
    }
    
    public List<Component> findComponentsOfType(String typeId) {
        List<Component> components  = new ArrayList<>();
        for (FlowStep flowStep : flowSteps) {
            if (flowStep.getComponent().getType().equals(typeId)) {
                components.add(flowStep.getComponent());
            }
        }
        return components;
    }

    public FlowStepLink findFlowStepLink(String sourceNodeId, String targetNodeId) {
        if (flowStepLinks != null) {
            for (FlowStepLink flowStepLink : flowStepLinks) {
                if (flowStepLink.getSourceStepId().equals(sourceNodeId)
                        && flowStepLink.getTargetStepId().equals(targetNodeId)) {
                    return flowStepLink;
                }
            }
        }
        return null;
    }

    public List<FlowStepLink> findFlowStepLinksWithSource(String sourceNodeId) {
        List<FlowStepLink> links = new ArrayList<FlowStepLink>();
        if (flowStepLinks != null) {
            for (FlowStepLink flowStepLink : flowStepLinks) {
                if (flowStepLink.getSourceStepId().equals(sourceNodeId)) {
                    links.add(flowStepLink);
                }
            }
        }
        return links;
    }
    
    public FlowStepLink findLinkBetweenSourceAndTarget(String sourceNodeId, String targetNodeId) {
        for (FlowStepLink flowStepLink : flowStepLinks) {
            if (flowStepLink.getTargetStepId().equals(targetNodeId)
                    && flowStepLink.getSourceStepId().equals(sourceNodeId)) {
                return flowStepLink;
            }
        }
        return null;
    }

    public List<FlowStepLink> findFlowStepLinksWithTarget(String targetNodeId) {
        List<FlowStepLink> links = new ArrayList<FlowStepLink>();
        if (flowStepLinks != null) {
            for (FlowStepLink flowStepLink : flowStepLinks) {
                if (flowStepLink.getTargetStepId().equals(targetNodeId)) {
                    links.add(flowStepLink);
                }
            }
        }
        return links;
    }

    public FlowStep findFlowStepWithId(String id) {
        for (FlowStep flowStep : flowSteps) {
            if (flowStep.getId().equals(id)) {
                return flowStep;
            }
        }
        return null;
    }

    public FlowStep findFlowStepWithComponentId(String id) {
        for (FlowStep flowStep : flowSteps) {
            if (flowStep.getComponentId().equals(id)) {
                return flowStep;
            }
        }
        return null;
    }

    public FlowStep findFlowStepWithName(String name) {
        for (FlowStep flowStep : flowSteps) {
            if (flowStep.getName().equals(name)) {
                return flowStep;
            }
        }
        return null;
    }

    public String getFolderName() {
        return folder.getName();
    }
    
    public void setFlowSteps(List<FlowStep> flowSteps) {
        this.flowSteps = flowSteps;
    }

    public List<FlowStep> getFlowSteps() {
        return flowSteps;
    }
    
    public void setFlowStepLinks(List<FlowStepLink> flowStepLinks) {
        this.flowStepLinks = flowStepLinks;
    }

    public List<FlowStepLink> getFlowStepLinks() {
        return flowStepLinks;
    }

    public FlowStep removeFlowStep(FlowStep flowStep) {
        Iterator<FlowStep> i = flowSteps.iterator();
        while (i.hasNext()) {
            FlowStep step = i.next();
            if (step.getId().equals(flowStep.getId())) {
                i.remove();
                return step;
            }
        }
        return null;
    }

    public List<FlowStepLink> removeFlowStepLinks(String flowStepId) {
        List<FlowStepLink> links = new ArrayList<FlowStepLink>();
        Iterator<FlowStepLink> i = flowStepLinks.iterator();
        while (i.hasNext()) {
            FlowStepLink link = i.next();
            if (link.getSourceStepId().equals(flowStepId)
                    || link.getTargetStepId().equals(flowStepId)) {
                i.remove();
                links.add(link);
            }
        }
        return links;
    }

    public FlowStepLink removeFlowStepLink(String sourceStepId, String targetStepId) {
        FlowStepLink link = null;
        Iterator<FlowStepLink> i = flowStepLinks.iterator();
        while (i.hasNext()) {
            link = i.next();
            if (link.getSourceStepId().equals(sourceStepId)
                    && link.getTargetStepId().equals(targetStepId)) {
                i.remove();
                break;
            }
        }
        return link;
    }

    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
        if (this.flowSteps != null) {
            for (FlowStep flowStep : flowSteps) {
                flowStep.getComponent().setProjectVersionId(projectVersionId);
            }
        }
    }

    public String getProjectVersionId() {
        return projectVersionId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public String getRowId() {
        return rowId;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public List<FlowParameter> getFlowParameters() {
        return flowParameters;
    }

    public void setFlowParameters(List<FlowParameter> flowParameters) {
        this.flowParameters = flowParameters;
    }
    
    public Map<String, String> toFlowParametersAsString() {
        Map<String, String> params = new HashMap<>();
        if (flowParameters != null) {
            for (FlowParameter flowParameter : flowParameters) {
                params.put(flowParameter.getName(), flowParameter.getDefaultValue());
            }
        }
        return params;
    }
    
    public void setTest(boolean test) {
        this.test = test;
    }
    
    public boolean isTest() {
        return test;
    }

    public void calculateApproximateOrder() {
        int order = 0;
        Collections.sort(flowSteps, new YSorter());
        List<FlowStep> starterSteps = findStartSteps();
        Set<FlowStep> visited = new HashSet<FlowStep>(flowSteps.size());
        for (FlowStep starterStep : starterSteps) {
            order = calculateApproximateOrder(order, starterStep, visited);
        }
    }
    
    protected int calculateApproximateOrder(int order, FlowStep starterStep, Set<FlowStep> visited) {
        if (!visited.add(starterStep)) {
            return order; // cycle detected
        }
        starterStep.setApproximateOrder(order++);
        
        List<FlowStep> children = new ArrayList<FlowStep>();
        for (FlowStepLink flowStepLink : flowStepLinks) {
            if (starterStep.getId().equals(flowStepLink.getSourceStepId())) {                
                FlowStep child = findFlowStepWithId(flowStepLink.getTargetStepId());
                children.add(child);
            }
        }
        Collections.sort(children, new YSorter());
        for (FlowStep child : children) {
            order = calculateApproximateOrder(order, child, visited);
        }
        return order;
    }

    public List<FlowStep> findStartSteps() {
        List<FlowStep> starterSteps = new ArrayList<FlowStep>();
        for (FlowStep flowStep : flowSteps) {
            boolean isTargetStep = false;
            for (FlowStepLink flowStepLink : flowStepLinks) {
                if (flowStep.getId().equals(flowStepLink.getTargetStepId())) {
                    isTargetStep = true;
                }
            }

            if (!isTargetStep) {
                starterSteps.add(flowStep);
            }
        }
        Collections.sort(starterSteps, new XSorter());
        Collections.sort(starterSteps, new YSorter());
        return starterSteps;
    }
    
    public List<FlowStep> findFinalSteps() {
        List<FlowStep> finalSteps = new ArrayList<FlowStep>();
        for (FlowStep flowStep : flowSteps) {
            boolean hasSourceLink = false;
            for (FlowStepLink flowStepLink : flowStepLinks) {
                if (flowStep.getId().equals(flowStepLink.getSourceStepId())) {
                    hasSourceLink = true;
                }
            }

            if (!hasSourceLink) {
                finalSteps.add(flowStep);
            }
        }
        return finalSteps;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String description) {
        this.notes = description;
    }

    static public class XSorter implements Comparator<FlowStep> {
        @Override
        public int compare(FlowStep o1, FlowStep o2) {
            return new Integer(o1.getX()).compareTo(new Integer(o2.getX()));
        }
    }
    
    static public class YSorter implements Comparator<FlowStep> {
        @Override
        public int compare(FlowStep o1, FlowStep o2) {
            return new Integer(o1.getY()).compareTo(new Integer(o2.getY()));
        }
    }
}
