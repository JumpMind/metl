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

import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.NOTES;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.plugin.XMLSetting;
import org.jumpmind.metl.core.runtime.IHttpRequestMappingRegistryAware;
import org.jumpmind.metl.core.runtime.flow.FlowRuntime;
import org.jumpmind.metl.core.runtime.web.HttpMethod;
import org.jumpmind.metl.core.runtime.web.HttpRequestMapping;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
import org.jumpmind.metl.core.util.GeneralUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestDeploymentListener implements IComponentDeploymentListener, IHttpRequestMappingRegistryAware {

    final Logger logger = LoggerFactory.getLogger(getClass());
    
    IHttpRequestMappingRegistry httpRequestMappingRegistry;

    @Override
    public void onDeploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep,
            XMLComponentDefinition componentDefinition) {
        if (flowStep.getComponent().getBoolean(ComponentSettingsConstants.ENABLED, true)) {
            HttpRequestMapping requestMapping = buildMapping(agent, agentProjectVersionFlowDeployment, flowStep, componentDefinition);
            if (!requestMapping.getPath().substring(0, 1).equalsIgnoreCase("/")) {
                requestMapping.setPath("/" + requestMapping.getPath());
            }
            httpRequestMappingRegistry.register(requestMapping);
        }
    }

    @Override
    public void onUndeploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep, XMLComponentDefinition componentDefinition) {
        httpRequestMappingRegistry.unregister(buildMapping(agent, agentProjectVersionFlowDeployment, flowStep, componentDefinition));
    }

    @Override
    public void setHttpRequestMappingRegistry(IHttpRequestMappingRegistry httpRequestMappingRegistry) {
        this.httpRequestMappingRegistry = httpRequestMappingRegistry;
    }

    protected HttpRequestMapping buildMapping(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep, XMLComponentDefinition componentDefinition) {
        TypedProperties properties = getTypedProperties(flowStep, componentDefinition);        
        String path = properties.get(HttpRequest.PATH);
        Map<String, String> replacements = FlowRuntime.getFlowParameters(agent, agentProjectVersionFlowDeployment);
        for(String key: replacements.keySet()) {
            String value = replacements.get(key);
            replacements.put(key, GeneralUtils.replaceSpecialCharacters(value));
        }
        path = FormatUtils.replaceTokens(path, replacements, true);
        
        String method = properties.get(HttpRequest.HTTP_METHOD, HttpMethod.GET.name());
        
        String responseDescription = null;
        List<Component> components = agentProjectVersionFlowDeployment.getFlow().findComponentsOfType(HttpResponse.TYPE);
        for (Component component : components) {
            String desc = component.get(NOTES);
            if (responseDescription == null) {
                responseDescription = desc;
            } else if (desc != null) {
                responseDescription += "\n" + desc;
            }
        }
        
        HttpRequestMapping mapping = new HttpRequestMapping();        
        mapping.setPath(path);
        mapping.setMethod(HttpMethod.valueOf(method));
        mapping.setSecurityScheme(SecurityScheme.valueOf(properties.get(HttpRequest.SECURITY_SCHEME, SecurityScheme.NONE.name())));
        mapping.setSecurityUsername(properties.get(HttpRequest.SECURE_USERNAME));
        mapping.setSecurityPassword(properties.get(HttpRequest.SECURE_PASSWORD));
        mapping.setRequestDescription(properties.get(NOTES));
        mapping.setFlowDescription(agentProjectVersionFlowDeployment.getFlow().getNotes());
        mapping.setResponseDescription(responseDescription);
        mapping.setDeployment(agentProjectVersionFlowDeployment.getAgentDeployment());
        return mapping;
    }

    protected TypedProperties getTypedProperties(FlowStep flowStep, XMLComponentDefinition componentDefinition) {
        List<XMLSetting> settings = componentDefinition != null ? componentDefinition.getSettings().getSetting() : null;
        if (settings == null) {
            settings = Collections.emptyList();
        }
        return flowStep.getComponent().toTypedProperties(settings);
    }

}
