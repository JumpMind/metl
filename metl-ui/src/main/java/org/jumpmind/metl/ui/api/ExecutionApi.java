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
import java.util.ArrayList;
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
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
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
import org.jumpmind.metl.core.util.GeneralUtils;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerMapping;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import springfox.documentation.annotations.ApiIgnore;

@Api(value = "Execution API", description = "This is the API for Metl")
@Controller
public class ExecutionApi {

    private static final String SWAGGER_JSON = "/swagger.json";
    
    private static final String SWAGGER_YAML = "/swagger.yaml";

    static final String WS = "/ws";

    final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    IAgentManager agentManager;

    @Autowired
    IExecutionService executionService;
    
    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IHttpRequestMappingRegistry requestRegistry;

    AntPathMatcher patternMatcher = new AntPathMatcher();

    @ApiOperation(
            value = "Invoke a flow that is deployed to an agent by name.  This is the way a non-webservice enabled flow is typically called by an external tool")
    @RequestMapping(value = "/agents/{agentName}/deployments/{deploymentName}/invoke", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final ExecutionResults invoke(@ApiParam(value = "The name of the agent to use") @PathVariable("agentName") String agentName,
            @ApiParam(value = "The name of the flow deployment to invoke") @PathVariable("deploymentName") String deploymentName,
            HttpServletRequest req) {
        return callFlow(agentName, deploymentName, null, req);
    }

    @ApiOperation(
            value = "Invoke a flow that is deployed to an agent by name.  This is the way a non-webservice enabled flow is typically called by an external tool")
    @RequestMapping(value = "/agents/{agentName}/deployments/{deploymentName}/versions/{versionName}/invoke", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final ExecutionResults invoke(@ApiParam(value = "The name of the agent to use") @PathVariable("agentName") String agentName,
            @ApiParam(value = "The name of the flow deployment to invoke") @PathVariable("deploymentName") String deploymentName,
            @ApiParam(value = "The version of the deployed flow to invoke") @PathVariable("versionName") String versionName,
            HttpServletRequest req) {
        return callFlow(agentName, deploymentName, versionName, req);
    }

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
    public final Object put(HttpServletRequest req, HttpServletResponse res, @RequestBody(required = false) String payload) throws Exception {
        return executeFlow(req, res, payload);
    }

    @ApiIgnore
    @RequestMapping(value = WS + "/**", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final Object delete(HttpServletRequest req, HttpServletResponse res, @RequestBody(required = false) String payload)
            throws Exception {
        return executeFlow(req, res, payload);
    }

    @ApiIgnore
    @RequestMapping(value = WS + "/**", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public final Object post(HttpServletRequest req, HttpServletResponse res, @RequestBody(required = false) String payload)
            throws Exception {
        return executeFlow(req, res, payload);
    }

    @ApiOperation(value = "This is the Json Swagger API definition for Metl Hosted Services. Visit http://swagger.io for more details about the specification")
    @RequestMapping(value = SWAGGER_JSON, method = RequestMethod.GET)
    public final void swaggerJson(HttpServletRequest req, HttpServletResponse res) throws Exception {
        Swagger swagger = buildSwagger(SWAGGER_JSON, req, res);
        res.getWriter().write(Json.pretty().writeValueAsString(swagger));
        res.getWriter().flush();
    }
        

    @ApiOperation(value = "This is the Yaml Swagger API definition for Metl Hosted Services.  Visit http://swagger.io for more details about the specification")
    @RequestMapping(value = SWAGGER_YAML, method = RequestMethod.GET)
    public final void swaggerYaml(HttpServletRequest req, HttpServletResponse res) throws Exception {
        Swagger swagger = buildSwagger(SWAGGER_YAML, req, res);
        res.getWriter().write(Yaml.pretty().writeValueAsString(swagger));
        res.getWriter().flush();
    }

    protected final Swagger buildSwagger(String type, HttpServletRequest req, HttpServletResponse res) throws Exception {
    Info info = new Info().version(VersionUtils.getCurrentVersion()).title("Metl Services")
                .description("Following is a list of deployed web services.   The listing is by agent.");
        String basePath = req.getContextPath() + req.getServletPath() + WS + req.getPathInfo();
        int index = basePath.indexOf(type);
        basePath = basePath.substring(0, index);
        
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add(MimeTypeUtils.APPLICATION_XML_VALUE);
        mimeTypes.add(MimeTypeUtils.APPLICATION_JSON_VALUE);
        
        Swagger swagger = new Swagger().info(info).host(req.getServerName() + ":" + req.getServerPort()).scheme(Scheme.HTTP)
                .basePath(basePath).produces(mimeTypes);

        Set<Agent> agents = agentManager.getAvailableAgents();
        for (Agent agent : agents) {
            Tag tag = new Tag().name(GeneralUtils.replaceSpecialCharacters(agent.getName()));
            if (agent.getName().startsWith("<")) {
                tag.setDescription("This is a development agent");
            } else {
                tag.setDescription("This is an agent");
            }
            swagger.addTag(tag);            

            List<AgentDeploy> deployments = agent.getAgentDeployments();
            for (AgentDeploy agentDeployment : deployments) {
                FlowName flow = configurationService.findFlowName(agentDeployment.getFlowId());
                if (flow.isWebService()) {
                    List<HttpRequestMapping> mappings = requestRegistry.getHttpRequestMappingsFor(agentDeployment);
                    for (HttpRequestMapping httpRequestMapping : mappings) {
                        Operation operation = new Operation().summary(flow.getName()).operationId(flow.getName()).tag(tag.getName())
                                .description(httpRequestMapping.getFlowDescription()).produces(mimeTypes);
                        String path = addParameters(operation, httpRequestMapping.getPath());
                        Response response = new Response().schema(new StringProperty()).description(httpRequestMapping.getResponseDescription());
                        operation.response(200, response);
                        switch (httpRequestMapping.getMethod()) {
                            case GET:
                                swagger.path(path, new Path().get(operation));
                                break;
                            case PUT:
                                operation.addParameter(new FormParameter().name("payload").required(false).property(new StringProperty()));
                                swagger.path(path, new Path().put(operation));
                                break;
                            case POST:
                                operation.addParameter(new FormParameter().name("payload").required(false).property(new StringProperty()));
                                swagger.path(path, new Path().post(operation));
                                break;
                            case DELETE:
                                swagger.path(path, new Path().delete(operation));
                                break;
                            case HEAD:
                                swagger.path(path, new Path().head(operation));
                                break;
                        }
                    }
                }
            }

        }

        return swagger;
    }

    protected String addParameters(Operation operation, String path) {
        String queryString = null;
        if (path.contains("?")) {
            String[] parts = path.split("?");
            path = parts[0];
            queryString = parts[1];

        }

        path = addParameters(operation, path, PathParameter.class);
        addParameters(operation, queryString, QueryParameter.class);

        return path;
    }

    protected String addParameters(Operation operation, String path, Class<? extends AbstractSerializableParameter<?>> type) {

        if (path != null) {
            StringBuilder finalPath = new StringBuilder();
            StringBuilder paramName = new StringBuilder();
            boolean tracking = false;
            for (int i = 0; i < path.length(); i++) {
                char c = path.charAt(i);
                if (c == '{') {
                    tracking = true;
                    paramName.setLength(0);
                    finalPath.append(c);
                } else if (tracking && c == '}') {
                    tracking = false;
                    addParameter(paramName.toString(), operation, type);
                    finalPath.append(c);
                } else if (c == '$' && path.charAt(i + 1) == '(') {
                    tracking = true;
                    paramName.setLength(0);
                    i++;
                    finalPath.append('{');
                } else if (tracking && c == ')') {
                    tracking = false;
                    addParameter(paramName.toString(), operation, type);
                    finalPath.append('}');
                } else if (tracking) {
                    paramName.append(c);
                    finalPath.append(c);
                } else {
                    finalPath.append(c);
                }
            }
            path = finalPath.toString();
        }
        return path;
    }

    private void addParameter(String name, Operation operation, Class<? extends AbstractSerializableParameter<?>> type) {
        try {
            AbstractSerializableParameter<?> param = type.newInstance();
            operation.addParameter(param.name(name).property(new StringProperty()));
        } catch (Exception e) {
            log.info("Failed to create parameter: " + name, e);
        }
    }

    private Object executeFlow(HttpServletRequest request, HttpServletResponse response, String payload) throws Exception {
        Object resultPayload = null;
        String requestType = request.getMethod();
        String restOfTheUrl = ((String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).substring(WS.length());
        log.info(String.format("Attempting to find a service uri match for %s with request type %s", restOfTheUrl, requestType));
        HttpRequestMapping mapping = requestRegistry.findBestMatch(HttpMethod.valueOf(requestType), restOfTheUrl);
        if (mapping != null) {
            Map<String, String> params = toMap(request);
            params.putAll(patternMatcher.extractUriTemplateVariables(mapping.getPath(), restOfTheUrl));
            if (isNotBlank(payload)) {
                params.put(REQUEST_VALUE_PARAMETER, payload.toString());
            }
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                params.put(headerName, request.getHeader(headerName));
            }
            AgentDeploy deployment = mapping.getDeployment();
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
                    } else if (isBlank(response.getContentType())) {
                        response.setContentType("application/octet-stream;charset=utf-8");
                    }
                    resultPayload = results.getValue();
                }
            }
            return resultPayload;

        } else {
            throw new CouldNotFindDeploymentException(
                    "Could not find a deployed web request that matches " + restOfTheUrl + " for an HTTP " + requestType);
        }
    }

    protected boolean enforceSecurity(IHasSecurity security, HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    protected boolean enforceBasicAuth(IHasSecurity security, HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean secured = true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();

                if (basic.equalsIgnoreCase("Basic")) {
                    try {
                        String credentials = new String(Base64.decodeBase64(st.nextToken().getBytes()));
                        int p = credentials.indexOf(":");
                        if (p != -1) {
                            String _username = credentials.substring(0, p).trim();
                            String _password = credentials.substring(p + 1).trim();
                            if (!security.getUsername().equals(_username) || !security.getPassword().equals(_password)) {
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
            userId = left(whereAreYou(req), 50);
        }
        return userId;
    }

    protected final ExecutionResults callFlow(String agentName, String deploymentName, String versionName, HttpServletRequest req) {
        agentName = decode(agentName);
        deploymentName = decode(deploymentName);
        Set<Agent> agents = agentManager.getAvailableAgents();
        Execution execution = null;
        boolean foundAgent = false;
        boolean foundDeployment = false;
        for (Agent agent : agents) {
            if (agent.getName().equals(agentName)) {
                foundAgent = true;
                List<AgentDeploy> deployments = agent.getAgentDeployments();
                for (AgentDeploy agentDeployment : deployments) {
                    if (agentDeployment.getName().equals(deploymentName)) {
                        if (versionName != null) {
                            Flow flow = configurationService.findFlow(agentDeployment.getFlowId());
                            ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
                            if (projectVersion.getVersionLabel().equalsIgnoreCase(versionName)) {
                                foundDeployment = true;
                            }
                        } else {
                            foundDeployment = true;
                        }
                        if (foundDeployment && agentDeployment.getDeploymentStatus() == DeploymentStatus.ENABLED) {
                            AgentRuntime agentRuntime = agentManager.getAgentRuntime(agent.getId());
                            String executionId = agentRuntime.scheduleNow(whoAreYou(req), agentDeployment, toMap(req));
                            boolean done = false;
                            do {
                                execution = executionService.findExecution(executionId);
                                done = execution != null && ExecutionStatus.isDone(execution.getExecutionStatus());
                                if (!done) {
                                    AppUtils.sleep(50);
                                }
                            } while (!done);
                            break;
                        }
                    }
                }
            }
        }

        if (execution != null) {
            ExecutionResults result = new ExecutionResults(execution.getId(), execution.getStatus(), execution.getStartTime(),
                    execution.getEndTime());
            if (execution.getExecutionStatus() == ExecutionStatus.ERROR) {
                List<ExecutionStep> steps = executionService.findExecutionSteps(execution.getId());
                for (ExecutionStep executionStep : steps) {
                    if (executionStep.getExecutionStatus() == ExecutionStatus.ERROR) {
                        List<ExecutionStepLog> logs = executionService.findExecutionStepLogsInError(executionStep.getId());
                        for (ExecutionStepLog executionStepLog : logs) {
                            if (executionStepLog.getLogLevel() == LogLevel.ERROR) {
                                result.setMessage(executionStepLog.getLogText());
                                break;
                            }
                        }
                    }
                }

//                throw new FailureException(result);
            }
            return result;
        } else {
            String msg = "Unexpected error";
            if (!foundAgent) {
                msg = String.format("Could not find an agent named '%s'", agentName);
            } else if (!foundDeployment) {
                msg = String.format("Could not find a deployment name '%s'", deploymentName);
            } else {
                msg = String.format("Found deployment '%s', but it was not enabled", deploymentName);
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
