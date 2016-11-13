package org.jumpmind.metl.core.runtime.component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.IHttpRequestMappingRegistryAware;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting;
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
    public void onDeploy(Agent agent, AgentDeployment deployment, Flow flow, FlowStep flowStep, XMLComponent componentDefinition) {
        HttpRequestMapping requestMapping = buildMapping(agent, deployment, flow, flowStep, componentDefinition);
        if (!requestMapping.getPath().substring(0,1).equalsIgnoreCase("/")) {
            requestMapping.setPath("/" + requestMapping.getPath());
        }
        httpRequestMappingRegistry.register(requestMapping);
    }

    @Override
    public void onUndeploy(Agent agent, AgentDeployment deployment, Flow flow, FlowStep flowStep, XMLComponent componentDefinition) {
        httpRequestMappingRegistry.unregister(buildMapping(agent, deployment, flow, flowStep, componentDefinition));
    }

    @Override
    public void setHttpRequestMappingRegistry(IHttpRequestMappingRegistry httpRequestMappingRegistry) {
        this.httpRequestMappingRegistry = httpRequestMappingRegistry;
    }

    protected HttpRequestMapping buildMapping(Agent agent, AgentDeployment deployment, Flow flow, FlowStep flowStep, XMLComponent componentDefinition) {
        TypedProperties properties = getTypedProperties(flowStep, componentDefinition);
        
        String path = properties.get(HttpRequest.PATH);
        Map<String, String> replacements = FlowRuntime.getFlowParameters(flow, agent, deployment);
        for(String key: replacements.keySet()) {
            String value = replacements.get(key);
            replacements.put(key, GeneralUtils.replaceSpecialCharacters(value));
        }
        path = FormatUtils.replaceTokens(path, replacements, true);
        
        String method = properties.get(HttpRequest.HTTP_METHOD, HttpMethod.GET.name());
        
        HttpRequestMapping mapping = new HttpRequestMapping();        
        mapping.setPath(path);
        mapping.setMethod(HttpMethod.valueOf(method));
        mapping.setSecurityScheme(SecurityScheme.valueOf(properties.get(HttpRequest.SECURITY_SCHEME, SecurityScheme.NONE.name())));
        mapping.setSecurityUsername(properties.get(HttpRequest.SECURE_USERNAME));
        mapping.setSecurityPassword(properties.get(HttpRequest.SECURE_PASSWORD));
        mapping.setDeployment(deployment);
        return mapping;
    }

    protected TypedProperties getTypedProperties(FlowStep flowStep, XMLComponent componentDefinition) {
        List<XMLSetting> settings = componentDefinition != null ? componentDefinition.getSettings().getSetting() : null;
        if (settings == null) {
            settings = Collections.emptyList();
        }
        return flowStep.getComponent().toTypedProperties(settings);
    }

}
