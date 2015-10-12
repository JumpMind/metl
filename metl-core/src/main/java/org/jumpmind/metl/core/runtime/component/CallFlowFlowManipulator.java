package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.List;

import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.IFlowManipulator;

public class CallFlowFlowManipulator implements IFlowManipulator {

    public CallFlowFlowManipulator() {
    }

    @Override
    public Flow manipulate(Flow flow, FlowStep flowStep, IConfigurationService configurationService) {
        flow = (Flow) flow.copy();

        String flowId = flowStep.getComponent().get(CallFlow.SETTING_FLOW_ID);
        if (isBlank(flowId)) {
            throw new MisconfiguredException(
                    "When using the 'Call Flow' component you must specify the flow you want to call.  It is currently blank");
        }

        Flow flowToCall = configurationService.findFlow(flowId);

        List<FlowStep> finalSteps = flow.getFlowSteps();
        List<FlowStepLink> finalLinks = flow.getFlowStepLinks();

        // remove source links to this step
        List<FlowStepLink> sourceLinksBefore = flow.findFlowStepLinksWithTarget(flowStep.getId());

        finalLinks.removeAll(sourceLinksBefore);

        List<FlowStep> flowToCallStartSteps = flowToCall.findStartSteps();

        List<FlowStep> flowToCallFinalSteps = flowToCall.findFinalSteps();

        // Add links and steps from the callable flow to this flow
        finalSteps.addAll(flowToCall.getFlowSteps());
        finalLinks.addAll(flowToCall.getFlowStepLinks());

        // Add links from source steps to this flow's startup steps
        for (FlowStep flowToCallStartStep : flowToCallStartSteps) {
            for (FlowStepLink beforeLinks : sourceLinksBefore) {
                finalLinks.add(new FlowStepLink(beforeLinks.getSourceStepId(), flowToCallStartStep.getId()));
            }
        }

        // Add links from last steps to the call flow step
        for (FlowStep flowToCallFinalStep : flowToCallFinalSteps) {
            finalLinks.add(new FlowStepLink(flowToCallFinalStep.getId(), flowStep.getId()));
        }

        return flow;
    }

}
