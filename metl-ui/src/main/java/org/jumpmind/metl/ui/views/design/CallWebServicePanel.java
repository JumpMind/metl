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
package org.jumpmind.metl.ui.views.design;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.runtime.component.SecurityScheme;
import org.jumpmind.metl.core.runtime.web.HttpRequestMapping;
import org.jumpmind.metl.ui.api.ApiConstants;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.IFlowRunnable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.views.manage.ExecutionRunPanel;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class CallWebServicePanel extends VerticalLayout implements IUiPanel, IFlowRunnable {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;

    AgentDeploy deployment;

    ReqRespTabSheet requestTabs;

    ReqRespTabSheet responseTabs;

    TextField urlField;

    OptionGroup methodGroup;

    VerticalLayout responseStatusAreaLayout;

    TextArea responseStatusArea;

    Button viewExecutionLogButton;

    String executionId;

    TabbedPanel tabs;

    ComboBox securitySchemeCombo;

    TextField userField;

    PasswordField passwordField;

    public CallWebServicePanel(AgentDeploy deployment, ApplicationContext context,
            TabbedPanel tabs) {
        this.deployment = deployment;
        this.context = context;
        this.tabs = tabs;
        IConfigurationService configurationService = context.getConfigurationService();
        Flow flow = configurationService.findFlow(deployment.getFlowId());

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.addButton("Call Service", Icons.RUN, (e) -> runFlow());

        viewExecutionLogButton = buttonBar.addButton("View Log", Icons.LOG, (e) -> openExecution());
        viewExecutionLogButton.setEnabled(false);

        addComponent(buttonBar);

        Panel scrollable = new Panel();
        scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
        scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        scrollable.setSizeFull();
        addComponent(scrollable);
        setExpandRatio(scrollable, 1);

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeUndefined();
        formLayout.setSpacing(true);
        formLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
        scrollable.setContent(formLayout);

        urlField = new TextField("URL");
        formLayout.addComponent(urlField);

        methodGroup = new OptionGroup("Method");
        methodGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        methodGroup.addItem("GET");
        methodGroup.addItem("PUT");
        methodGroup.addItem("POST");
        methodGroup.addItem("DELETE");
        formLayout.addComponent(methodGroup);

        ComboBox contentType = new ComboBox("Content Type");
        contentType.addItem(MimeTypeUtils.APPLICATION_JSON.toString());
        contentType.addItem(MimeTypeUtils.APPLICATION_XML.toString());
        contentType.setNullSelectionAllowed(false);
        formLayout.addComponent(contentType);

        securitySchemeCombo = new ComboBox("Security Scheme");
        securitySchemeCombo.setNullSelectionAllowed(false);
        SecurityScheme[] types = SecurityScheme.values();
        for (SecurityScheme securityType : types) {
            securitySchemeCombo.addItem(securityType);
        }
        securitySchemeCombo.addValueChangeListener((e) -> securityMethodChanged());
        formLayout.addComponent(securitySchemeCombo);

        userField = new TextField("Security Username");
        userField.setNullRepresentation("");
        userField.setVisible(false);
        formLayout.addComponent(userField);

        passwordField = new PasswordField("Security Password");
        passwordField.setNullRepresentation("");
        passwordField.setVisible(false);
        formLayout.addComponent(passwordField);

        requestTabs = new ReqRespTabSheet("Request", true);
        formLayout.addComponent(requestTabs);

        responseTabs = new ReqRespTabSheet("Response", false);
        responseStatusAreaLayout = new VerticalLayout();
        responseStatusAreaLayout.setSizeFull();
        responseStatusAreaLayout.setCaption("Status");
        responseStatusArea = new TextArea();
        responseStatusArea.setSizeFull();
        responseStatusAreaLayout.addComponent(responseStatusArea);
        responseTabs.addTab(responseStatusAreaLayout, 0);
        formLayout.addComponent(responseTabs);

        contentType.addValueChangeListener((e) -> {
            requestTabs.setHeader(HttpHeaders.CONTENT_TYPE, (String) contentType.getValue());
            requestTabs.setHeader(HttpHeaders.ACCEPT, (String) contentType.getValue());
        });

        List<HttpRequestMapping> mappings = context.getHttpRequestMappingRegistry()
                .getHttpRequestMappingsFor(deployment);
        if (mappings.size() > 0) {
            HttpRequestMapping mapping = mappings.get(0);
            try {
                ServletContext servletContext = VaadinServlet.getCurrent().getServletContext();
                String contextPath = servletContext.getContextPath();
                String pageUrl = Page.getCurrent().getLocation().toURL().toExternalForm();
                String url = pageUrl.substring(0,
                        pageUrl.indexOf(contextPath) + contextPath.length());
                urlField.setValue(
                        String.format("%s/api/ws%s", url, mapping.getPath().startsWith("/")
                                ? mapping.getPath() : ("/" + mapping.getPath())));
            } catch (MalformedURLException e1) {
            }

            securitySchemeCombo.setValue(mapping.getSecurityScheme());
            userField.setValue(mapping.getSecurityUsername());
            passwordField.setValue(mapping.getSecurityPassword());
            
            methodGroup.setValue(mapping.getMethod().name());

            List<FlowStep> steps = flow.getFlowSteps();
            contentType.select(MimeTypeUtils.APPLICATION_JSON.toString());
            for (FlowStep flowStep : steps) {
                String format = flowStep.getComponent().findSetting("format").getValue();
                if ("XML".equals(format)) {
                    contentType.select(MimeTypeUtils.APPLICATION_XML.toString());
                    break;
                }
            }

        } else {
            contentType.select(contentType.getItemIds().iterator().next());
            methodGroup.setValue("GET");
        }

    }

    protected void securityMethodChanged() {
        SecurityScheme scheme = (SecurityScheme) securitySchemeCombo.getValue();
        boolean visible = scheme != SecurityScheme.NONE;
        userField.setVisible(visible);
        passwordField.setVisible(visible);
    }

    protected void openExecution() {
        if (isNotBlank(executionId)) {
            ExecutionRunPanel logPanel = new ExecutionRunPanel(executionId, context, tabs, this);
            logPanel.onBackgroundUIRefresh(logPanel.onBackgroundDataRefresh());
            tabs.addCloseableTab(executionId, "Run " + deployment.getName(), Icons.LOG,
                    logPanel);
        }
    }

    @Override
    public void runFlow() {
        Map<String, String> headerMap;
        try {
            viewExecutionLogButton.setEnabled(false);
            RestTemplate template = null;            
            if (securitySchemeCombo.getValue() == SecurityScheme.BASIC) {
                template = new RestTemplate(new BasicRequestFactory(userField.getValue(), passwordField.getValue()));
            } else {
                template = new RestTemplate();
            }
            HttpHeaders headers = new HttpHeaders();
            headerMap = requestTabs.getHeaders();
            for (String key : headerMap.keySet()) {
                headers.add(key, headerMap.get(key));
            }
            HttpEntity<String> entity = new HttpEntity<>(requestTabs.getPayload().getValue(),
                    headers);
            ResponseEntity<String> response = template.exchange(urlField.getValue(),
                    HttpMethod.valueOf((String) methodGroup.getValue()), entity, String.class);
            responseTabs.getPayload().setValue(response.getBody());
            headerMap = response.getHeaders().toSingleValueMap();
            for (String key : headerMap.keySet()) {
                responseTabs.setHeader(key, headerMap.get(key));
            }
            responseStatusArea.setValue(response.getStatusCode().toString() + " "
                    + response.getStatusCode().getReasonPhrase());
        } catch (HttpStatusCodeException e) {
            responseTabs.getPayload().setValue(e.getResponseBodyAsString());
            headerMap = e.getResponseHeaders().toSingleValueMap();
            for (String key : headerMap.keySet()) {
                responseTabs.setHeader(key, headerMap.get(key));
            }
            responseStatusArea.setValue(
                    e.getStatusCode().toString() + " " + e.getStatusCode().getReasonPhrase());
        }

        if (headerMap != null) {
            executionId = headerMap.get(ApiConstants.HEADER_EXECUTION_ID);
            if (isNotBlank(executionId)) {
                viewExecutionLogButton.setEnabled(true);
            }
        }

        if (isBlank(responseTabs.getPayload().getValue())) {
            responseTabs.setSelectedTab(0);
        } else {
            responseTabs.setSelectedTab(1);
        }

    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    @Override
    public boolean closing() {
        return true;
    }

    class ReqRespTabSheet extends TabSheet {
        private static final long serialVersionUID = 1L;
        VerticalLayout payloadLayout;
        TextArea payload;
        Grid headersGrid;

        public ReqRespTabSheet(String caption, boolean editable) {
            setCaption(caption);
            setHeight(15, Unit.EM);
            setWidth(550, Unit.PIXELS);
            addStyleName(ValoTheme.TABSHEET_COMPACT_TABBAR);

            payloadLayout = new VerticalLayout();
            payloadLayout.setSizeFull();
            payload = new TextArea();
            payload.setNullRepresentation("");
            payload.setSizeFull();
            payloadLayout.addComponent(payload);
            addTab(payloadLayout, "Payload");

            VerticalLayout requestHeadersLayout = new VerticalLayout();
            requestHeadersLayout.setSizeFull();
            if (editable) {
                HorizontalLayout requestHeadersButtonLayout = new HorizontalLayout();
                Button addButton = new Button("+", (e) -> add());
                addButton.addStyleName(ValoTheme.BUTTON_SMALL);
                Button deleteButton = new Button("-", (e) -> delete());
                deleteButton.addStyleName(ValoTheme.BUTTON_SMALL);
                requestHeadersButtonLayout.addComponent(addButton);
                requestHeadersButtonLayout.addComponent(deleteButton);
                requestHeadersLayout.addComponent(requestHeadersButtonLayout);
            }

            headersGrid = new Grid();
            headersGrid.setEditorEnabled(editable);
            headersGrid.setEditorSaveCaption("Save");
            headersGrid.setEditorCancelCaption("Cancel");
            headersGrid.setSelectionMode(SelectionMode.SINGLE);
            headersGrid.setSizeFull();
            headersGrid.addColumn("headerName").setHeaderCaption("Header").setEditable(true)
                    .setExpandRatio(1);
            headersGrid.addColumn("headerValue").setHeaderCaption("Value").setEditable(true)
                    .setExpandRatio(1);
            requestHeadersLayout.addComponent(headersGrid);
            requestHeadersLayout.setExpandRatio(headersGrid, 1);
            addTab(requestHeadersLayout, "Headers");
        }

        public VerticalLayout getPayloadLayout() {
            return payloadLayout;
        }

        @SuppressWarnings("unchecked")
        protected void setHeader(String name, String value) {
            Indexed container = headersGrid.getContainerDataSource();
            Collection<?> itemIds = container.getItemIds();
            boolean found = false;
            for (Object itemId : itemIds) {
                Item item = container.getItem(itemId);
                if (name.equals(item.getItemProperty("headerName").getValue())) {
                    item.getItemProperty("headerValue").setValue(value);
                    found = true;
                    break;
                }
            }

            if (!found) {
                Object itemId = container.addItem();
                Item item = container.getItem(itemId);
                item.getItemProperty("headerName").setValue(name);
                item.getItemProperty("headerValue").setValue(value);
            }
        }

        protected Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<String, String>();
            Indexed container = headersGrid.getContainerDataSource();
            Collection<?> itemIds = container.getItemIds();
            for (Object itemId : itemIds) {
                Item item = container.getItem(itemId);
                headers.put((String) item.getItemProperty("headerName").getValue(),
                        (String) item.getItemProperty("headerValue").getValue());
            }
            return headers;
        }

        protected void add() {
            headersGrid.getContainerDataSource().addItem();
        }

        protected void delete() {
            Object selected = headersGrid.getSelectedRow();
            if (selected != null) {
                headersGrid.getContainerDataSource().removeItem(selected);
            }
        }

        public TextArea getPayload() {
            return payload;
        }

        public Grid getHeadersGrid() {
            return headersGrid;
        }

    }
    
    public class BasicRequestFactory extends HttpComponentsClientHttpRequestFactory {

        public BasicRequestFactory(String username, String password) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            setHttpClient(HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build());
        }

        @Override
        protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
            HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);
            BasicHttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
            return localContext;
        }

    }

}
