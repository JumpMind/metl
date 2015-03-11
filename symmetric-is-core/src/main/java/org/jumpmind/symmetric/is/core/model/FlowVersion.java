package org.jumpmind.symmetric.is.core.model;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FlowVersion extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Flow flow;

    List<FlowStep> flowSteps;

    List<FlowStepLink> flowStepLinks;
    
    String startType = StartType.MANUAL.name();
    
    String startExpression;
    
    String versionName = "";

    public FlowVersion() {
        this.flowSteps = new ArrayList<FlowStep>();
        this.flowStepLinks = new ArrayList<FlowStepLink>();
    }
    
    public FlowVersion(Flow flow) {
        this();
        setFlow(flow);
    }

    public FlowStep findFlowStepWithId(String id) {
        for (FlowStep flowStep : flowSteps) {
            if (flowStep.getId().equals(id)) {
                return flowStep;
            }
        }
        return null;
    }
    
    public void setFlowId(String flowId) {
        if (flowId != null) {
            this.flow = new Flow(flowId);
        } else {
            this.flow = null;
        }
    }

    public String getFlowId() {
        return flow != null ? flow.getId() : null;
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
        return flow.getFolder().getName();
    }

    public void setVersionName(String versionName) {        
        this.versionName = versionName;
    }

    public String getVersionName() {        
        return versionName;
    }

    public List<FlowStep> getFlowSteps() {
        return flowSteps;
    }

    public Flow getFlow() {
        return flow;
    }
    
    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    public List<FlowStepLink> getFlowStepLinks() {
        return flowStepLinks;
    }
    
    public void setName(String name) {
        setVersionName(name);
    }
    
    public String getName() {
        return versionName;
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
        Iterator<FlowStepLink> i = flowStepLinks.iterator();
        while (i.hasNext()) {
            FlowStepLink link = i.next();
            if (link.getSourceStepId().equals(sourceStepId)
                    && link.getTargetStepId().equals(targetStepId)) {
                i.remove();
                return link;
            }
        }
        return null;
    }
    
    public StartType asStartType() {
        if (isBlank(startType)) {
            return StartType.MANUAL;
        } else {
            return StartType.valueOf(startType);
        }
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
}
