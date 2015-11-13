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
package org.jumpmind.metl.ui.api;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Api(value = "Execution API", description = "This is the API for Metl")
@Controller
public class ExecutionApi {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    @Autowired
    IAgentManager agentManager;

    @Autowired
    IExecutionService executionService;

    @ApiOperation(value = "Invoke a manipulatedFlow that is deployed to an agent by name")
    @RequestMapping(
            value = "/agents/{agentName}/deployments/{deploymentName}/invoke",
            method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final ExecutionResults invoke(
            @ApiParam(value = "The name of the agent to use") @PathVariable("agentName") String agentName,
            @ApiParam(value = "The name of the manipulatedFlow deployment to invoke") @PathVariable("deploymentName") String deploymentName,
            HttpServletRequest req) {
        agentName = decode(agentName);
        deploymentName = decode(deploymentName);
        Set<Agent> agents = agentManager.getAvailableAgents();
        Execution execution = null;
        boolean foundAgent = false;
        boolean foundDeployment = false;
        for (Agent agent : agents) {
            if (agent.getName().equals(agentName)) {
                foundAgent = true;
                List<AgentDeployment> deployments = agent.getAgentDeployments();
                for (AgentDeployment agentDeployment : deployments) {
                    if (agentDeployment.getName().equals(deploymentName)) {
                        foundDeployment = true;
                        AgentRuntime agentRuntime = agentManager.getAgentRuntime(agent);
                        String executionId = agentRuntime.scheduleNow(agentDeployment, toObjectMap(req));
                        boolean done = false;
                        do {
                            execution = executionService.findExecution(executionId);
                            done = execution != null
                                    && ExecutionStatus.isDone(execution.getExecutionStatus());
                            if (!done) {
                                AppUtils.sleep(5000);
                            }
                        } while (!done);
                        break;
                    }
                }
            }
        }

        if (execution != null) {
            ExecutionResults result = new ExecutionResults(execution.getId(), execution.getStatus(),
                    execution.getStartTime(), execution.getEndTime());
            if (execution.getExecutionStatus() == ExecutionStatus.ERROR) {
                List<ExecutionStep> steps = executionService.findExecutionSteps(execution.getId());
                for (ExecutionStep executionStep : steps) {
                    if (executionStep.getExecutionStatus() == ExecutionStatus.ERROR) {
                        List<ExecutionStepLog> logs = executionService.findExecutionStepLogs(executionStep.getId());
                        for (ExecutionStepLog executionStepLog : logs) {
                            if (executionStepLog.getLogLevel() == LogLevel.ERROR) {
                                result.setMessage(executionStepLog.getLogText());
                                break;
                            }
                        }
                    }
                }
            }
            return result;
        } else {
            String msg = "Unexpected error";
            if (!foundAgent) {
                msg = String.format("Could not find an agent named '%s'", agentName);
            } else if (!foundDeployment) {
                msg = String.format("Could not find a deployment name '%s'", deploymentName);
            }
            throw new CouldNotFindDeploymentException(msg);
        }
    }

    protected String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected Map<String, String> toObjectMap(HttpServletRequest req) {
        Map<String, String> params = new HashMap<String, String>();
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, req.getParameter(name));
        }
        return params;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public RestError handleError(Exception ex, HttpServletRequest req) {
        log.error("Web service call failed with error", ex);
        int httpErrorCode = 500;
        Annotation annotation = ex.getClass().getAnnotation(ResponseStatus.class);
        if (annotation != null) {
            httpErrorCode = ((ResponseStatus) annotation).value().value();
        }
        return new RestError(ex, httpErrorCode);
    }

}
