package org.jumpmind.symmetric.is.core.model;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Flow extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String rowId = UUID.randomUUID().toString();
    
    Folder folder;
    
    String projectVersionId;

    String name;

    List<FlowStep> flowSteps;

    List<FlowStepLink> flowStepLinks;

    String startType = StartType.MANUAL.name();

    String startExpression;
    
    boolean deleted = false;

    public Flow() {
        this.flowSteps = new ArrayList<FlowStep>();
        this.flowStepLinks = new ArrayList<FlowStepLink>();
    }

    public Flow(Folder folder) {
        this();
        setFolder(folder);
    }

    public Flow(String id) {
        this();
        this.id = id;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Folder getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

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

    public FlowStep findFlowStepWithId(String id) {
        for (FlowStep flowStep : flowSteps) {
            if (flowStep.getId().equals(id)) {
                return flowStep;
            }
        }
        return null;
    }

    public void setStartExpression(String startExpression) {
        this.startExpression = startExpression;
    }

    public String getStartExpression() {
        return startExpression;
    }

    public void setStartType(String startType) {
        this.startType = startType;
    }

    public String getStartType() {
        return startType;
    }

    public String getFolderName() {
        return folder.getName();
    }

    public List<FlowStep> getFlowSteps() {
        return flowSteps;
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
            }
        }
        return link;
    }

    public StartType asStartType() {
        if (isBlank(startType)) {
            return StartType.MANUAL;
        } else {
            return StartType.valueOf(startType);
        }
    }
    
    public void setProjectVersionId(String projectVersionId) {
        this.projectVersionId = projectVersionId;
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
}
