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
package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.IFlowManipulator;

public class EmbedFlowFlowManipulator implements IFlowManipulator {

    public EmbedFlowFlowManipulator() {
    }

    @Override
    public Flow manipulate(Flow flow, FlowStep flowStep, IConfigurationService configurationService) {

        Component comp = flowStep.getComponent();
        if (comp.getBoolean(AbstractComponentRuntime.ENABLED, true)) {
            String flowId = comp.get(EmbedFlow.SETTING_FLOW_ID);
            if (isBlank(flowId)) {
                throw new MisconfiguredException(
                        "When using the 'Call Flow' component you must specify the flow you want to call.  It is currently blank");
            }

            Flow flowToCall = configurationService.findFlow(flowId);

            List<FlowStep> finalSteps = flow.getFlowSteps();
            List<FlowStepLink> finalLinks = flow.getFlowStepLinks();

            // remove source links to this step
            List<FlowStepLink> beforeLinks = flow.findFlowStepLinksWithTarget(flowStep.getId());
            finalLinks.removeAll(beforeLinks);

            List<FlowStep> flowToCallStartSteps = flowToCall.findStartSteps();

            List<FlowStep> flowToCallFinalSteps = flowToCall.findFinalSteps();

            // Add links and steps from the callable flow to this flow
            finalSteps.addAll(flowToCall.getFlowSteps());
            finalLinks.addAll(flowToCall.getFlowStepLinks());

            // Add links from source steps to this flow's startup steps
            for (FlowStep flowToCallStartStep : flowToCallStartSteps) {
                for (FlowStepLink beforeLink : beforeLinks) {
                    finalLinks.add(new FlowStepLink(beforeLink.getSourceStepId(), flowToCallStartStep.getId()));
                }
            }

            // Add links from last steps to the call flow step
            for (FlowStep flowToCallFinalStep : flowToCallFinalSteps) {
                finalLinks.add(new FlowStepLink(flowToCallFinalStep.getId(), flowStep.getId()));
            }
        }
        return flow;
    }

}
