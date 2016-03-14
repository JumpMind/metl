package org.jumpmind.metl.core.runtime.component;

import java.util.Collections;
import java.util.List;

import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.IHttpRequestMappingRegistryAware;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting;
import org.jumpmind.metl.core.runtime.web.HttpMethod;
import org.jumpmind.metl.core.runtime.web.HttpRequestMapping;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
import org.jumpmind.properties.TypedProperties;

public class HttpRequestDeploymentListener implements IComponentDeploymentListener, IHttpRequestMappingRegistryAware {

    IHttpRequestMappingRegistry httpRequestMappingRegistry;

    @Override
    public void onDeploy(AgentDeployment deployment, FlowStep flowStep, XMLComponent componentDefinition) {
        httpRequestMappingRegistry.register(buildMapping(deployment, flowStep, componentDefinition));
    }

    @Override
    public void onUndeploy(AgentDeployment deployment, FlowStep flowStep, XMLComponent componentDefinition) {
        httpRequestMappingRegistry.unregister(buildMapping(deployment, flowStep, componentDefinition));
    }

    @Override
    public void setHttpRequestMappingRegistry(IHttpRequestMappingRegistry httpRequestMappingRegistry) {
        this.httpRequestMappingRegistry = httpRequestMappingRegistry;
    }

    protected HttpRequestMapping buildMapping(AgentDeployment deployment, FlowStep flowStep, XMLComponent componentDefinition) {
        TypedProperties properties = getTypedProperties(flowStep, componentDefinition);
        String path = properties.get(HttpRequest.PATH);
        String method = properties.get(HttpRequest.HTTP_METHOD, HttpMethod.GET.name());
        HttpRequestMapping mapping = new HttpRequestMapping();
        mapping.setPath(path);
        mapping.setMethod(HttpMethod.valueOf(method));
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
