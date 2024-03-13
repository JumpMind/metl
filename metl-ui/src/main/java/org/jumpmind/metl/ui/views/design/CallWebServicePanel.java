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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
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
import org.jumpmind.vaadin.ui.common.TabSheet;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.VaadinServlet;

public class CallWebServicePanel extends VerticalLayout implements IUiPanel, IFlowRunnable {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;

    AgentDeploy deployment;

    ReqRespTabSheet requestTabs;

    ReqRespTabSheet responseTabs;

    TextField urlField;

    RadioButtonGroup<String> methodGroup;

    TextArea responseStatusArea;

    Button viewExecutionLogButton;

    String executionId;

    TabbedPanel tabs;

    ComboBox<SecurityScheme> securitySchemeCombo;

    TextField userField;

    PasswordField passwordField;

    public CallWebServicePanel(AgentDeploy deployment, ApplicationContext context,
            TabbedPanel tabs) {
        this.deployment = deployment;
        this.context = context;
        this.tabs = tabs;
        IConfigurationService configurationService = context.getConfigurationService();
        Flow flow = configurationService.findFlow(deployment.getFlowId());
        
        setPadding(false);
        setSpacing(false);

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.addButton("Call Service", Icons.RUN, (e) -> runFlow());

        viewExecutionLogButton = buttonBar.addButton("View Log", Icons.LOG, (e) -> openExecution());
        viewExecutionLogButton.setEnabled(false);

        add(buttonBar);

        Scroller scrollable = new Scroller();
        scrollable.setSizeFull();
        add(scrollable);
        expand(scrollable);

        FormLayout formLayout = new FormLayout();
        formLayout.setSizeUndefined();
        formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
        formLayout.getStyle().set("padding-left", "16px");
        scrollable.setContent(formLayout);

        urlField = new TextField();
        urlField.setWidth("550px");
        formLayout.addFormItem(urlField, "URL");

        methodGroup = new RadioButtonGroup<String>();
        methodGroup.setItems("GET", "PUT", "POST", "DELETE");
        formLayout.addFormItem(methodGroup, "Method");

        ComboBox<String> contentType = new ComboBox<String>();
        contentType.setWidth("550px");
        contentType.setItems(MimeTypeUtils.APPLICATION_JSON.toString(), MimeTypeUtils.APPLICATION_XML.toString());
        formLayout.addFormItem(contentType, "Content Type");

        securitySchemeCombo = new ComboBox<SecurityScheme>();
        securitySchemeCombo.setWidth("550px");
        securitySchemeCombo.setItems(SecurityScheme.values());
        securitySchemeCombo.addValueChangeListener((e) -> securityMethodChanged(e));
        formLayout.addFormItem(securitySchemeCombo, "Security Scheme");

        userField = new TextField();
        userField.setWidth("550px");
        userField.setVisible(false);
        formLayout.addFormItem(userField, "Security Username");

        passwordField = new PasswordField();
        passwordField.setWidth("550px");
        passwordField.setVisible(false);
        formLayout.addFormItem(passwordField, "Security Password");

        requestTabs = new ReqRespTabSheet(true);
        formLayout.addFormItem(requestTabs, "Request");

        responseTabs = new ReqRespTabSheet(false);
        responseStatusArea = new TextArea();
        responseStatusArea.setSizeFull();
        responseTabs.add(responseStatusArea, "Status", 0);
        responseTabs.setSelectedTab(1);
        formLayout.addFormItem(responseTabs, "Response");

        contentType.addValueChangeListener((e) -> {
            if (e.getValue() != null) {
                requestTabs.setHeader(HttpHeaders.CONTENT_TYPE, (String) contentType.getValue());
                requestTabs.setHeader(HttpHeaders.ACCEPT, (String) contentType.getValue());
            } else {
                contentType.setValue(e.getOldValue());
            }
        });

        List<HttpRequestMapping> mappings = context.getHttpRequestMappingRegistry()
                .getHttpRequestMappingsFor(deployment);
        if (mappings.size() > 0) {
            HttpRequestMapping mapping = mappings.get(0);
            ServletContext servletContext = VaadinServlet.getCurrent().getServletContext();
            String contextPath = servletContext.getContextPath();
            UI.getCurrent().getPage().fetchCurrentURL(url -> {
                String pageUrl = url.toExternalForm();
                String urlString = pageUrl.substring(0, pageUrl.indexOf(contextPath) + contextPath.length());
                urlField.setValue(String.format("%s/api/ws%s", urlString,
                        mapping.getPath().startsWith("/") ? mapping.getPath() : ("/" + mapping.getPath())));
            });

            securitySchemeCombo.setValue(mapping.getSecurityScheme());
            userField.setValue(mapping.getSecurityUsername());
            passwordField.setValue(mapping.getSecurityPassword());
            
            methodGroup.setValue(mapping.getMethod().name());

            List<FlowStep> steps = flow.getFlowSteps();
            contentType.setValue(MimeTypeUtils.APPLICATION_JSON.toString());
            for (FlowStep flowStep : steps) {
                String format = flowStep.getComponent().findSetting("format").getValue();
                if ("XML".equals(format)) {
                    contentType.setValue(MimeTypeUtils.APPLICATION_XML.toString());
                    break;
                }
            }

        } else {
            contentType.setValue(MimeTypeUtils.APPLICATION_JSON.toString());
            methodGroup.setValue("GET");
        }

    }

    protected void securityMethodChanged(ValueChangeEvent<SecurityScheme> event) {
        if (event.getValue() != null) {
            SecurityScheme scheme = (SecurityScheme) securitySchemeCombo.getValue();
            boolean visible = scheme != SecurityScheme.NONE;
            userField.setVisible(visible);
            passwordField.setVisible(visible);
        } else {
            securitySchemeCombo.setValue(event.getOldValue());
        }
    }

    protected void openExecution() {
        if (isNotBlank(executionId)) {
            ExecutionRunPanel logPanel = new ExecutionRunPanel(executionId, context, tabs, this);
            logPanel.onBackgroundUIRefresh(logPanel.onBackgroundDataRefresh());
            tabs.addCloseableTab(executionId, "Run " + deployment.getName(), new Icon(Icons.LOG),
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
                    HttpMethod.valueOf(methodGroup.getValue()), entity, String.class);
            responseTabs.getPayload().setValue(response.getBody());
            headerMap = response.getHeaders().toSingleValueMap();
            for (String key : headerMap.keySet()) {
                responseTabs.setHeader(key, headerMap.get(key));
            }
            responseStatusArea.setValue(response.getStatusCode().toString() + " "
                    + ((HttpStatus) response.getStatusCode()).getReasonPhrase());
        } catch (HttpStatusCodeException e) {
            responseTabs.getPayload().setValue(e.getResponseBodyAsString());
            headerMap = e.getResponseHeaders().toSingleValueMap();
            for (String key : headerMap.keySet()) {
                responseTabs.setHeader(key, headerMap.get(key));
            }
            responseStatusArea.setValue(
                    e.getStatusCode().toString() + " " + ((HttpStatus) e.getStatusCode()).getReasonPhrase());
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
        TextArea payload;
        List<Header> headersList = new ArrayList<Header>();
        Grid<Header> headersGrid;

        public ReqRespTabSheet(boolean editable) {
            setHeight("15em");
            setWidth("550px");
            addThemeVariants(TabsVariant.LUMO_SMALL);
            payload = new TextArea();
            payload.setSizeFull();
            add(payload, "Payload");

            VerticalLayout requestHeadersLayout = new VerticalLayout();
            requestHeadersLayout.setSizeFull();
            requestHeadersLayout.setPadding(false);
            requestHeadersLayout.setSpacing(false);
            if (editable) {
                HorizontalLayout requestHeadersButtonLayout = new HorizontalLayout();
                Button addButton = new Button(new Icon(VaadinIcon.PLUS), (e) -> add());
                addButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
                Button deleteButton = new Button(new Icon(VaadinIcon.MINUS), (e) -> delete());
                deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
                requestHeadersButtonLayout.add(addButton, deleteButton);
                requestHeadersLayout.add(requestHeadersButtonLayout);
            }

            headersGrid = new Grid<Header>();
            if (editable) {
                Editor<Header> headersEditor = headersGrid.getEditor();
                Binder<Header> binder = new Binder<Header>();
                headersEditor.setBinder(binder);
                TextField headerField = new TextField();
                headerField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                binder.forField(headerField).bind(Header::getName, Header::setName);
                headersGrid.addColumn(Header::getName).setHeader("Header").setEditorComponent(headerField)
                        .setFlexGrow(1);
                TextField valueField = new TextField();
                valueField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                binder.forField(valueField).bind(Header::getValue, Header::setValue);
                headersGrid.addColumn(Header::getValue).setHeader("Value").setEditorComponent(valueField)
                        .setFlexGrow(1);
            	
                headersGrid.addItemDoubleClickListener(event -> headersEditor.editItem(event.getItem()));
            } else {
                headersGrid.addColumn(Header::getName).setHeader("Header").setFlexGrow(1);
                headersGrid.addColumn(Header::getValue).setHeader("Value").setFlexGrow(1);
            }
            headersGrid.setSelectionMode(SelectionMode.SINGLE);
            headersGrid.setSizeFull();
            headersGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
            requestHeadersLayout.addAndExpand(headersGrid);
            add(requestHeadersLayout, "Headers");
        }

        protected void setHeader(String name, String value) {
            boolean found = false;
            for (Header header : headersList) {
                if (name.equals(header.getName())) {
                    header.setValue(value);
                    found = true;
                    break;
                }
            }

            if (!found) {
                headersList.add(new Header(name, value));
                headersGrid.setItems(headersList);
            }
        }

        protected Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<String, String>();
            for (Header header : headersList) {
            	headers.put(header.getName(), header.getValue());
            }
            return headers;
        }

        protected void add() {
            headersList.add(new Header());
            headersGrid.setItems(headersList);
        }

        protected void delete() {
            Header selected = headersGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
            if (selected != null) {
				headersList = headersList.stream().filter(header -> !StringUtils.equals(header.getName(), selected.getName()))
						.collect(Collectors.toList());
                headersGrid.setItems(headersList);
            }
        }

        public TextArea getPayload() {
            return payload;
        }

        public Grid<Header> getHeadersGrid() {
            return headersGrid;
        }

        public class Header {
        	private String name;
        	private String value;
        	
        	public Header() {
        	}
        	
        	public Header(String name, String value) {
        		this.name = name;
        		this.value = value;
        	}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getValue() {
				return value;
			}

			public void setValue(String value) {
				this.value = value;
			}
        }
    }
    
    public class BasicRequestFactory extends HttpComponentsClientHttpRequestFactory {

        public BasicRequestFactory(String username, String password) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(username, password.toCharArray()));
            setHttpClient(HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build());
        }

        @Override
        protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
            HttpHost targetHost = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
            BasicAuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);
            BasicHttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
            return localContext;
        }

    }

}
