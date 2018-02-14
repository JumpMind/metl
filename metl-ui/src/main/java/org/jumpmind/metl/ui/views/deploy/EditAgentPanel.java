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
package org.jumpmind.metl.ui.views.deploy;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.AgentResource;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.init.BackgroundRefresherService;
import org.jumpmind.metl.ui.views.design.CallWebServicePanel;
import org.jumpmind.metl.ui.views.manage.ExecutionRunPanel;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.jumpmind.vaadin.ui.common.NotifyDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.DefaultItemSorter;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditAgentPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable<Object>, IAgentDeploymentChangeListener {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    Agent agent;

    Table table;

    BeanItemContainer<AgentDeploymentSummary> container;

    Button addDeploymentButton;

    Button enableButton;

    Button disableButton;

    Button removeButton;

    Button editButton;

    Button runButton;

    TextField filterField;

    BackgroundRefresherService backgroundRefresherService;

    public EditAgentPanel(ApplicationContext context, TabbedPanel tabbedPanel, Agent agent) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;
        this.agent = agent;
        this.backgroundRefresherService = context.getBackgroundRefresherService();

        HorizontalLayout editAgentLayout = new HorizontalLayout();
        editAgentLayout.setSpacing(true);
        editAgentLayout.setMargin(new MarginInfo(true, false, false, true));
        editAgentLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
        addComponent(editAgentLayout);

        Button parameterButton = new Button("Parameters");
        parameterButton.addClickListener(new ParameterClickListener());
        editAgentLayout.addComponent(parameterButton);
        editAgentLayout.setComponentAlignment(parameterButton, Alignment.BOTTOM_LEFT);

        TextField executionThreadsField = new ImmediateUpdateTextField("Execution Threads") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void save(String text) {
                try {
                    int value = Integer.parseInt(text);
                    agent.setExecThreadCount(value);
                    context.getOperationsService().save((AbstractObject) agent);
                    EditAgentPanel.this.context.getAgentManager().refresh(agent);
                } catch (NumberFormatException ex) {
                    NotifyDialog.show("Number required", "Please enter a valid number", null, Type.WARNING_MESSAGE);
                }
            }
        };
        executionThreadsField.setValue(Integer.toString(agent.getExecThreadCount()));
        editAgentLayout.addComponent(executionThreadsField);
        editAgentLayout.setComponentAlignment(executionThreadsField, Alignment.BOTTOM_LEFT);

        CheckBox autoRefresh = new CheckBox("Refresh?", Boolean.valueOf(agent.isAutoRefresh()));
        autoRefresh.setImmediate(true);
        autoRefresh.setDescription("Automatically refresh flow from database before execution?");
        autoRefresh.addValueChangeListener(event -> {
            agent.setAutoRefresh(autoRefresh.getValue());
            EditAgentPanel.this.context.getOperationsService().save((AbstractObject) agent);
            EditAgentPanel.this.context.getAgentManager().refresh(agent);
        });
        editAgentLayout.addComponent(autoRefresh);
        editAgentLayout.setComponentAlignment(autoRefresh, Alignment.BOTTOM_LEFT);

        CheckBox showInExploreViewField = new CheckBox("Explore?", Boolean.valueOf(agent.isShowResourcesInExploreView()));
        showInExploreViewField.setDescription("Show resources deployed to this agent in the explore view");
        showInExploreViewField.setImmediate(true);
        showInExploreViewField.addValueChangeListener(event -> {
            agent.setShowResourcesInExploreView(showInExploreViewField.getValue());
            EditAgentPanel.this.context.getOperationsService().save((AbstractObject) agent);
            EditAgentPanel.this.context.getAgentManager().refresh(agent);
        });
        editAgentLayout.addComponent(showInExploreViewField);
        editAgentLayout.setComponentAlignment(showInExploreViewField, Alignment.BOTTOM_LEFT);

        CheckBox allowTestFlowsField = new CheckBox("Allow Tests?", Boolean.valueOf(agent.isAllowTestFlows()));
        allowTestFlowsField.setDescription("Allow test flows to be deployed to this agent");
        allowTestFlowsField.setImmediate(true);
        allowTestFlowsField.addValueChangeListener(event -> {
            agent.setAllowTestFlows(allowTestFlowsField.getValue());
            EditAgentPanel.this.context.getOperationsService().save((AbstractObject) agent);
            EditAgentPanel.this.context.getAgentManager().refresh(agent);
        });
        editAgentLayout.addComponent(allowTestFlowsField);
        editAgentLayout.setComponentAlignment(allowTestFlowsField, Alignment.BOTTOM_LEFT);

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        addDeploymentButton = buttonBar.addButton("Deploy", Icons.DEPLOYMENT);
        addDeploymentButton.addClickListener(e->new DeployDialog(context, this).show());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(event -> editClicked());

        enableButton = buttonBar.addButton("Enable", FontAwesome.CHAIN);
        enableButton.addClickListener(event -> enableClicked());

        disableButton = buttonBar.addButton("Disable", FontAwesome.CHAIN_BROKEN);
        disableButton.addClickListener(event -> disableClicked());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(event -> removeClicked());

        runButton = buttonBar.addButton("Run", Icons.RUN);
        runButton.addClickListener(event -> runClicked());

        container = new BeanItemContainer<AgentDeploymentSummary>(AgentDeploymentSummary.class);
        container.setItemSorter(new TableItemSorter());

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(event -> {
            filterField.setValue(event.getText());
            refresh();
        });

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "projectName", "projectVersionLabel", "type", "status", "logLevel", "startType", "startExpression");
        table.setColumnHeaders("Deployment", "Project", "Version", "Type", "Status", "Log Level", "Start Type", "Start Expression");
        table.addGeneratedColumn("status", new StatusRenderer());
        table.addItemClickListener(new TableItemClickListener());
        table.addValueChangeListener(new TableValueChangeListener());
        table.setSortContainerPropertyId("type");
        table.setSortAscending(true);

        addComponent(table);
        setExpandRatio(table, 1.0f);
        refresh();
        setButtonsEnabled();        
        backgroundRefresherService.register(this);
    }
    
    public Agent getAgent() {
        return agent;
    }

    @Override
    public boolean closing() {
        backgroundRefresherService.unregister(this);
        return true;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    public void changed(AgentDeploy agentDeployment) {
        for (AgentDeploymentSummary summary : container.getItemIds()) {
            if (summary.getId().equals(agentDeployment.getId())) {
                summary.copy(agentDeployment);
            }
        }
    }

    @Override
    public Object onBackgroundDataRefresh() {
        return getRefreshData();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        updateItems((List<AgentDeploymentSummary>) backgroundData);
    }

    @Override
    public void onUIError(Throwable ex) {
        CommonUiUtils.notify(ex);        
    }
    
    protected List<AgentDeploymentSummary> getRefreshData() {
        List<AgentDeploymentSummary> summaries = context.getOperationsService().findAgentDeploymentSummary(agent.getId());
        String filterString = filterField.getValue();
        if (isNotBlank(filterString)) {
            filterString = filterString.toLowerCase();
            Iterator<AgentDeploymentSummary> it = summaries.iterator();
            while (it.hasNext()) {
                AgentDeploymentSummary type = it.next();
                if (!type.getName().toLowerCase().contains(filterString)) {
                    it.remove();
                }
            }
        }
        return summaries;
    }

    public void refresh() {
        updateItems(getRefreshData());
    }

    protected void updateItem(AgentDeploymentSummary summary) {
        Set<AgentDeploymentSummary> selectedItems = getSelectedItems();
        container.removeItem(summary);
        container.addItem(summary);
        table.sort();
        setSelectedItems(selectedItems);
        setButtonsEnabled();
    }

    protected void updateItems(List<AgentDeploymentSummary> summaries) {
        Set<AgentDeploymentSummary> selectedItems = getSelectedItems();
        boolean isChanged = container.size() != summaries.size();
        for (AgentDeploymentSummary summary : summaries) {
            BeanItem<AgentDeploymentSummary> beanItem = container.getItem(summary);
            if (beanItem == null || beanItem.getBean().isChanged(summary)) {
                container.removeItem(summary);
                container.addItem(summary);
                isChanged = true;
            }
        }
        Set<AgentDeploymentSummary> items = new HashSet<AgentDeploymentSummary>(container.getItemIds());
        for (AgentDeploymentSummary summary : items) {
            if (!summaries.contains(summary)) {
                container.removeItem(summary);
                isChanged = true;
            }
        }
        if (isChanged) {
            table.sort();
            setSelectedItems(selectedItems);
            setButtonsEnabled();
        }
    }

    protected void setButtonsEnabled() {
        boolean canRemove = false;
        boolean canEnable = false;
        boolean canDisable = false;
        boolean canRun = false;
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                if (summary.getStatus().equals(DeploymentStatus.ENABLED.name())
                        || summary.getStatus().equals(DeploymentStatus.DISABLED.name())
                        || summary.getStatus().equals(DeploymentStatus.ERROR.name())) {
                    canRemove = true;
                }
                if (summary.getStatus().equals(DeploymentStatus.ENABLED.name())) {
                    canDisable = true;
                    if (summary.isFlow()) {
                        canRun = true;
                    }
                }
                if (summary.getStatus().equals(DeploymentStatus.DISABLED.name())
                        || summary.getStatus().equals(DeploymentStatus.ERROR.name())) {
                    canEnable = true;
                }
            }
        }
        runButton.setEnabled(canRun && selectedIds.size() == 1);
        enableButton.setEnabled(canEnable);
        disableButton.setEnabled(canDisable);
        removeButton.setEnabled(canRemove);
        editButton.setEnabled(getSelectedItems().size() > 0);
    }

    @SuppressWarnings("unchecked")
    protected Set<AgentDeploymentSummary> getSelectedItems() {
        return (Set<AgentDeploymentSummary>) table.getValue();
    }
    
    protected List<AgentDeploymentSummary> getAgentDeploymentSummary() {
        return container.getItemIds();
    }

    protected void setSelectedItems(Set<AgentDeploymentSummary> selectedItems) {
        table.setValue(null);
        for (AgentDeploymentSummary summary : selectedItems) {
            BeanItem<AgentDeploymentSummary> beanItem = container.getItem(summary);
            if (beanItem != null) {
                AgentDeploymentSummary updatedSummary = beanItem.getBean();
                table.select(updatedSummary);
            }
        }
    }

    protected void runClicked() {
        AgentDeploymentSummary summary = (AgentDeploymentSummary) getSelectedItems().iterator().next();
        if (summary.isFlow()) {
            AgentDeploy deployment = context.getOperationsService().findAgentDeployment(summary.getId());
            IAgentManager agentManager = context.getAgentManager();
            FlowName flow = context.getConfigurationService().findFlowName(deployment.getFlowId());
            if (flow.isWebService()) {
                CallWebServicePanel panel = new CallWebServicePanel(deployment, context, tabbedPanel);
                tabbedPanel.addCloseableTab(deployment.getId(), "Call " + deployment.getName(), Icons.RUN, panel);
            } else {
                String executionId = agentManager.getAgentRuntime(deployment.getAgentId()).scheduleNow(context.getUser().getLoginId(),
                        deployment);
                if (executionId != null) {
                    ExecutionRunPanel logPanel = new ExecutionRunPanel(executionId, context, tabbedPanel, null);
                    tabbedPanel.addCloseableTab(executionId, "Run " + deployment.getName(), Icons.LOG, logPanel);
                }
            }
        }
    }

    protected void editClicked() {
        AgentDeploymentSummary summary = (AgentDeploymentSummary) getSelectedItems().iterator().next();
        if (summary.isFlow()) {
            AgentDeploy deployment = context.getOperationsService().findAgentDeployment(summary.getId());
            EditAgentDeploymentPanel editPanel = new EditAgentDeploymentPanel(context, deployment, EditAgentPanel.this, tabbedPanel);
            tabbedPanel.addCloseableTab(deployment.getId(), deployment.getName(), Icons.DEPLOYMENT, editPanel);
        } else {
            Resource resource = context.getConfigurationService().findResource(summary.getId());
            AgentResource agentResource = context.getOperationsService().findAgentResource(agent.getId(), resource);
            EditAgentResourcePanel editPanel = new EditAgentResourcePanel(context, agentResource);
            FontAwesome icon = Icons.GENERAL_RESOURCE;
            if (agentResource.getType().equals("Database")) {
                icon = Icons.DATABASE;
            } else if (agentResource.getType().equals("Local File System")) {
                icon = Icons.FILE_SYSTEM;
            }
            tabbedPanel.addCloseableTab(summary.getId(), summary.getName(), icon, editPanel);
        }
    }

    protected void enableClicked() {
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                AgentDeploy deployment = context.getOperationsService().findAgentDeployment(summary.getId());
                deployment.setStatus(DeploymentStatus.REQUEST_ENABLE.name());
                summary.setStatus(DeploymentStatus.REQUEST_ENABLE.name());
                context.getOperationsService().save(deployment);
                updateItem(summary);
            }
        }
    }

    protected void disableClicked() {
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                AgentDeploy deployment = context.getOperationsService().findAgentDeployment(summary.getId());
                deployment.setStatus(DeploymentStatus.REQUEST_DISABLE.name());
                summary.setStatus(DeploymentStatus.REQUEST_DISABLE.name());
                context.getOperationsService().save(deployment);
                updateItem(summary);
            }
        }
    }

    protected void removeClicked() {
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                AgentDeploy deployment = context.getOperationsService().findAgentDeployment(summary.getId());
                deployment.setStatus(DeploymentStatus.REQUEST_REMOVE.name());
                summary.setStatus(DeploymentStatus.REQUEST_REMOVE.name());
                context.getOperationsService().save(deployment);
                updateItem(summary);
            }
        }
    }

    class StatusRenderer implements ColumnGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public Object generateCell(Table source, Object itemId, Object columnId) {
            String status = itemId != null ? ((AgentDeploymentSummary) itemId).getStatus() : null;
            return status != null ? DeploymentStatus.valueOf(status).toString() : null;
        }
    }

    class TableItemClickListener implements ItemClickListener {
        private static final long serialVersionUID = 1L;

        long lastClick;

        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItemId()) && System.currentTimeMillis() - lastClick > 500) {
                table.setValue(null);
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TableValueChangeListener implements ValueChangeListener {
        private static final long serialVersionUID = 1L;

        public void valueChange(ValueChangeEvent event) {
            setButtonsEnabled();
        }
    }

    class TableItemSorter extends DefaultItemSorter {
        private static final long serialVersionUID = 1L;

        Object[] propertyId;

        boolean[] ascending;

        public void setSortProperties(Sortable container, Object[] propertyId, boolean[] ascending) {
            super.setSortProperties(container, propertyId, ascending);
            this.propertyId = propertyId;
            this.ascending = ascending;
        }

        public int compare(Object o1, Object o2) {
            AgentDeploymentSummary s1 = (AgentDeploymentSummary) o1;
            AgentDeploymentSummary s2 = (AgentDeploymentSummary) o2;
            if (propertyId != null && propertyId.length > 0 && propertyId[0].equals("projectName")) {
                return new CompareToBuilder().append(s1.getProjectName(), s2.getProjectName()).append(s1.getName(), s2.getName())
                        .toComparison() * (ascending[0] ? 1 : -1);
            }
            return super.compare(o1, o2);
        }
    }

    class ParameterClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;

        public void buttonClick(ClickEvent event) {
            EditAgentParametersDialog window = new EditAgentParametersDialog(context, agent);
            window.showAtSize(0.5);
        }
    }

}
