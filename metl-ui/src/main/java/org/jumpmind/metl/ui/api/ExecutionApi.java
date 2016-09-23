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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.left;
import static org.jumpmind.metl.core.runtime.FlowConstants.REQUEST_VALUE_PARAMETER;
import static org.jumpmind.metl.ui.api.ApiConstants.HEADER_EXECUTION_ID;
import static org.jumpmind.metl.ui.common.UiUtils.whereAreYou;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.component.IHasSecurity;
import org.jumpmind.metl.core.runtime.component.Results;
import org.jumpmind.metl.core.runtime.flow.FlowRuntime;
import org.jumpmind.metl.core.runtime.web.HttpMethod;
import org.jumpmind.metl.core.runtime.web.HttpRequestMapping;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerMapping;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import springfox.documentation.annotations.ApiIgnore;

@Api(value = "Execution API", description = "This is the API for Metl")
@Controller
public class ExecutionApi {

    static final String WS = "/ws";

    final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    IAgentManager agentManager;

    @Autowired
    IExecutionService executionService;

    @Autowired
    IHttpRequestMappingRegistry requestRegistry;

    AntPathMatcher patternMatcher = new AntPathMatcher();

    @ApiIgnore
    @RequestMapping(value = WS + "/**", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final Object get(HttpServletRequest req, HttpServletResponse res) throws Exception {
        return executeFlow(req, res, null);
    }

    @ApiIgnore
    @RequestMapping(value = WS + "/**", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final Object put(HttpServletRequest req, HttpServletResponse res,
            @RequestBody(required = false) String payload) throws Exception {
        return executeFlow(req, res, payload);
    }

    @ApiIgnore
    @RequestMapping(value = WS + "/**", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final Object delete(HttpServletRequest req, HttpServletResponse res,
            @RequestBody(required = false) String payload) throws Exception {
        return executeFlow(req, res, payload);
    }

    @ApiIgnore
    @RequestMapping(value = WS + "/**", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final Object post(HttpServletRequest req, HttpServletResponse res,
            @RequestBody(required = false) String payload) throws Exception {
        return executeFlow(req, res, payload);
    }

    private Object executeFlow(HttpServletRequest request, HttpServletResponse response,
            String payload) throws Exception {
        Object resultPayload = null;
        String requestType = request.getMethod();
        String restOfTheUrl = ((String) request
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
                        .substring(WS.length());
        log.info(String.format("Attempting to find a service uri match for %s with request type %s",
                restOfTheUrl, requestType));
        HttpRequestMapping mapping = requestRegistry.findBestMatch(HttpMethod.valueOf(requestType),
                restOfTheUrl);
        if (mapping != null) {
            Map<String, String> params = toMap(request);
            params.putAll(
                    patternMatcher.extractUriTemplateVariables(mapping.getPath(), restOfTheUrl));
            if (isNotBlank(payload)) {
                params.put(REQUEST_VALUE_PARAMETER, payload.toString());
            }
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                params.put(headerName, request.getHeader(headerName));
            }
            AgentDeployment deployment = mapping.getDeployment();
            AgentRuntime agentRuntime = agentManager.getAgentRuntime(deployment.getAgentId());
            FlowRuntime flowRuntime = agentRuntime.createFlowRuntime(whoAreYou(request), deployment, params);
            IHasSecurity security = flowRuntime.getHasSecurity();
            if (enforceSecurity(security, request, response)) {
                String executionId = flowRuntime.getExecutionId();
                response.setHeader(HEADER_EXECUTION_ID, executionId);
                Results results = flowRuntime.execute();
                if (results != null) {
                    String contentType = results.getContentType();
                    if (isNotBlank(contentType)) {
                        response.setContentType(contentType);
                    }
                    resultPayload = results.getValue();
                }                
            }
            return resultPayload;

        } else {
            throw new CouldNotFindDeploymentException(
                    "Could not find a deployed web request that matches " + restOfTheUrl
                            + " for an HTTP " + requestType);
        }
    }

    protected boolean enforceSecurity(IHasSecurity security, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        boolean secured = true;
        if (security != null) {
            switch (security.getSecurityType()) {
                case BASIC:
                    secured = enforceBasicAuth(security, request, response);
                    break;
                case NONE:
                default:
                    break;
            }
        }
        return secured;
    }

    protected boolean enforceBasicAuth(IHasSecurity security, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        boolean secured = true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();

                if (basic.equalsIgnoreCase("Basic")) {
                    try {
                        String credentials = new String(Base64.decodeBase64(st.nextToken()),
                                "UTF-8");
                        int p = credentials.indexOf(":");
                        if (p != -1) {
                            String _username = credentials.substring(0, p).trim();
                            String _password = credentials.substring(p + 1).trim();
                            if (!security.getUsername().equals(_username)
                                    || !security.getPassword().equals(_password)) {
                                unauthorized(response, "Bad credentials");
                                secured = false;
                            }

                        } else {
                            unauthorized(response, "Invalid authentication token");
                            secured = false;
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Couldn't retrieve authentication", e);
                    }
                }
            }
        } else {
            unauthorized(response);
            secured = false;
        }
        return secured;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"Protected\"");
        response.sendError(401, message);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        unauthorized(response, "Unauthorized");
    }
    

    
    private String whoAreYou(HttpServletRequest req) {
        String userId = left(req.getRemoteUser(), 50);
        if (isBlank(userId)) {
            userId =  left(whereAreYou(req), 50);
        }
        return userId;
    }

    @ApiOperation(value = "Invoke a flow that is deployed to an agent by name")
    @RequestMapping(value = "/agents/{agentName}/deployments/{deploymentName}/invoke", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final ExecutionResults invoke(
            @ApiParam(value = "The name of the agent to use") @PathVariable("agentName") String agentName,
            @ApiParam(value = "The name of the flow deployment to invoke") @PathVariable("deploymentName") String deploymentName,
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
                        if (agentDeployment.getDeploymentStatus() == DeploymentStatus.ENABLED) {
                            AgentRuntime agentRuntime = agentManager.getAgentRuntime(agent.getId());
                            String executionId = agentRuntime.scheduleNow(whoAreYou(req), agentDeployment,
                                    toMap(req));
                            boolean done = false;
                            do {
                                execution = executionService.findExecution(executionId);
                                done = execution != null
                                        && ExecutionStatus.isDone(execution.getExecutionStatus());
                                if (!done) {
                                    AppUtils.sleep(500);
                                }
                            } while (!done);
                            break;
                        }
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
                        List<ExecutionStepLog> logs = executionService
                                .findExecutionStepLogsInError(executionStep.getId());
                        for (ExecutionStepLog executionStepLog : logs) {
                            if (executionStepLog.getLogLevel() == LogLevel.ERROR) {
                                result.setMessage(executionStepLog.getLogText());
                                break;
                            }
                        }
                    }
                }
                
                throw new FailureException(result);
            }
            return result;
        } else {
            String msg = "Unexpected error";
            if (!foundAgent) {
                msg = String.format("Could not find an agent named '%s'", agentName);
            } else if (!foundDeployment) {
                msg = String.format("Could not find a deployment name '%s'", deploymentName);
            } else {
                msg = String.format("Found deployment '%s', but it was not enabled",
                        deploymentName);
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

    protected Map<String, String> toMap(HttpServletRequest req) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        Enumeration<String> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, req.getParameter(name));
        }
        return params;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public RestError handleError(Exception ex, HttpServletRequest req, HttpServletResponse resp) {
        log.error("Web service call failed with error", ex);
        int httpErrorCode = 500;
        Annotation annotation = ex.getClass().getAnnotation(ResponseStatus.class);
        if (annotation != null) {
            httpErrorCode = ((ResponseStatus) annotation).value().value();
        }
        resp.setStatus(httpErrorCode);
        return new RestError(ex, httpErrorCode);
    }

}
