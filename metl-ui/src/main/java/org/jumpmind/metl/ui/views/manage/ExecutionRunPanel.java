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
package org.jumpmind.metl.ui.views.manage;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.UserSetting;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.metl.ui.diagram.Node;
import org.jumpmind.metl.ui.diagram.NodeSelectedEvent;
import org.jumpmind.metl.ui.diagram.RunDiagram;
import org.jumpmind.metl.ui.views.IFlowRunnable;
import org.jumpmind.util.AppUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ReadOnlyTextAreaDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.data.sort.SortOrder;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.themes.ValoTheme;

public class ExecutionRunPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable {

    private static final long serialVersionUID = 1L;

    final protected Logger log = LoggerFactory.getLogger(getClass());

    IExecutionService executionService;

    VerticalSplitPanel splitPanel;

    Table stepTable = new Table();

    RunDiagram diagram;

    Panel flowPanel;

    AbstractLayout diagramLayout;

    Flow flow;

    List<AbstractObject> selected = new ArrayList<AbstractObject>();

    Grid logTable;

    BeanContainer<String, ExecutionStep> stepContainer = new BeanContainer<String, ExecutionStep>(ExecutionStep.class);

    BeanItemContainer<ExecutionStepLog> logContainer = new BeanItemContainer<ExecutionStepLog>(ExecutionStepLog.class);

    Label flowLabel = new Label();

    Label statusLabel = new Label("", ContentMode.HTML);

    Label startLabel = new Label();

    Label endLabel = new Label();

    Button removeButton;

    Button cancelButton;

    Button rerunButton;

    Button showDiagram;

    Button showDetails;

    String executionId;

    ApplicationContext context;

    TabbedPanel parentTabSheet;

    IFlowRunnable flowRunnable;

    boolean lastDataRefreshWasDone = false;

    List<SortOrder> lastSortOrder;

    public ExecutionRunPanel(String executionId, ApplicationContext context, TabbedPanel parentTabSheet, IFlowRunnable flowRunnable) {
        this.executionService = context.getExecutionService();
        this.executionId = executionId;
        this.context = context;
        this.parentTabSheet = parentTabSheet;
        this.flowRunnable = flowRunnable;

        Execution execution = executionService.findExecution(executionId);
        this.flow = context.getConfigurationService().findFlow(execution.getFlowId());

        ButtonBar buttonBar = new ButtonBar();

        rerunButton = buttonBar.addButton("Rerun", Icons.RUN, event -> rerun());
        rerunButton.setVisible(false);
        removeButton = buttonBar.addButton("Remove", Icons.DELETE, event -> remove());
        removeButton.setVisible(false);
        cancelButton = buttonBar.addButton("Cancel", Icons.CANCEL, event -> cancel());

        showDiagram = buttonBar.addButtonRight("Show Diagram", Icons.FLOW, event -> showDiagram());
        showDetails = buttonBar.addButtonRight("Show Details", Icons.LIST, event -> showDetails());

        addComponent(buttonBar);

        HorizontalLayout header1 = new HorizontalLayout();
        header1.addComponent(new Label("<b>Flow:</b>", ContentMode.HTML));
        header1.addComponent(flowLabel);
        header1.addComponent(new Label("<b>Start:</b>", ContentMode.HTML));
        header1.addComponent(startLabel);
        header1.setSpacing(true);
        header1.setMargin(new MarginInfo(false, true, false, true));
        header1.setWidth("100%");
        addComponent(header1);

        HorizontalLayout header2 = new HorizontalLayout();
        header2.addComponent(new Label("<b>Status:</b>", ContentMode.HTML));
        header2.addComponent(statusLabel);
        header2.addComponent(new Label("<b>End:</b>", ContentMode.HTML));
        header2.addComponent(endLabel);
        header2.setSpacing(true);
        header2.setMargin(new MarginInfo(false, true, true, true));
        header2.setWidth("100%");
        addComponent(header2);

        stepContainer.setBeanIdProperty("id");

        diagramLayout = new VerticalLayout();
        diagramLayout.setWidth(10000, Unit.PIXELS);
        diagramLayout.setHeight(10000, Unit.PIXELS);

        flowPanel = new Panel();
        flowPanel.setSizeFull();
        flowPanel.addStyleName(ValoTheme.PANEL_WELL);
        
        flowPanel.setContent(diagramLayout);

        stepTable.setContainerDataSource(stepContainer);
        stepTable.setSelectable(true);
        stepTable.setMultiSelect(true);
        stepTable.setImmediate(true);
        stepTable.setSizeFull();
        stepTable.setVisibleColumns(new Object[] { "componentName", "threadNumber", "status", "payloadReceived", "messagesReceived",
                "messagesProduced", "payloadProduced", "startTime", "endTime", "handleDurationString" });
        stepTable.setColumnHeaders(new String[] { "Component Name", "Thread", "Status", "Payload Recvd", "Msgs Recvd", "Msgs Sent",
                "Payload Sent", "Start", "End", "Run Duration" });
        stepTable.setColumnWidth("status", 100);
        stepTable.setColumnWidth("messagesReceived", 100);
        stepTable.setColumnWidth("messagesProduced", 100);
        stepTable.setColumnWidth("payloadReceived", 100);
        stepTable.setColumnWidth("payloadProduced", 100);
        stepTable.setColumnWidth("threadNumber", 100);
        stepTable.setColumnWidth("startTime", 100);
        stepTable.setColumnWidth("endTime", 100);
        stepTable.setColumnExpandRatio("handleDurationString", 1);
        stepTable.addValueChangeListener(event -> {
            @SuppressWarnings("unchecked")
            Set<String> executionStepIds = (Set<String>) event.getProperty().getValue();
            logContainer.removeAllItems();
            List<ExecutionStepLog> logs = executionService.findExecutionStepLogs(executionStepIds);
            logContainer.addAll(logs);
        });

        stepTable.addValueChangeListener(event -> {
            @SuppressWarnings("unchecked")
            Set<String> executionStepIds = (Set<String>) event.getProperty().getValue();
            logContainer.removeAllItems();
            List<ExecutionStepLog> logs = executionService.findExecutionStepLogs(executionStepIds);
            logContainer.addAll(logs);
        });

        logTable = new Grid();
        logTable.addColumn("level", String.class).setHeaderCaption("Level").setWidth(110).setMaximumWidth(200);
        logTable.addColumn("createTime", Date.class).setHeaderCaption("Time").setWidth(120).setMaximumWidth(200)
                .setRenderer(new DateRenderer("%1$tk:%1$tM:%1$tS:%1$tL"));
        logTable.addColumn("logText", String.class).setHeaderCaption("Message").setExpandRatio(1);
        logTable.setContainerDataSource(logContainer);
        logTable.setSizeFull();
        logTable.addItemClickListener(event -> logTableCellClicked(logTable, event));
        logTable.addSortListener(event -> {
            lastSortOrder = event.getSortOrder();
        });

        HeaderRow filteringHeader = logTable.appendHeaderRow();
        HeaderCell logTextFilterCell = filteringHeader.getCell("logText");
        TextField filterField = new TextField();
        filterField.setInputPrompt("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        filterField.setWidth("100%");

        // Update filter When the filter input is changed
        filterField.addTextChangeListener(change -> {
            // Can't modify filters so need to replace
            logContainer.removeContainerFilters("logText");

            // (Re)create the filter if necessary
            if (!change.getText().isEmpty())
                logContainer.addContainerFilter(new SimpleStringFilter("logText", change.getText(), true, false));
        });
        logTextFilterCell.setComponent(filterField);

        HeaderCell levelFilterCell = filteringHeader.getCell("level");
        ComboBox levelFilter = new ComboBox();
        levelFilter.setWidth(8, Unit.EM);
        levelFilter.setNullSelectionAllowed(true);
        LogLevel[] levels = LogLevel.values();
        for (LogLevel logLevel : levels) {
            levelFilter.addItem(logLevel.name());
        }
        levelFilter.addValueChangeListener(change -> {
            logContainer.removeContainerFilters("level");
            String text = (String) levelFilter.getValue();
            if (isNotBlank(text)) {
                logContainer.addContainerFilter(new SimpleStringFilter("level", text, true, false));
            }
        });
        levelFilterCell.setComponent(levelFilter);

        levelFilter.addStyleName(ValoTheme.COMBOBOX_TINY);

        splitPanel = new VerticalSplitPanel();
        splitPanel.setFirstComponent(flowPanel);
        splitPanel.setSecondComponent(logTable);
        splitPanel.setSplitPosition(50f);
        splitPanel.setSizeFull();
        addComponent(splitPanel);
        setExpandRatio(splitPanel, 1.0f);

        boolean showDiagram = context.getUser().getBoolean(UserSetting.SETTING_SHOW_RUN_DIAGRAM, true);
        if (showDiagram) {
            showDiagram();
        } else {
            showDetails();
        }

        redrawFlow();
        context.getBackgroundRefresherService().register(this);
    }

    protected void redrawFlow() {
        if (diagram != null) {
            diagramLayout.removeComponent(diagram);
        }
        diagram = new RunDiagram();
        diagram.setSizeFull();
        diagram.addListener(new RunDiagramChangedListener());
        diagram.setNodes(getNodes());
        diagramLayout.addComponent(diagram);
    }

    protected List<Node> getNodes() {
        ExecutionData executionData = getExecutionData();
        List<FlowStep> flowSteps = flow.getFlowSteps();
        List<FlowStepLink> links = flow.getFlowStepLinks();
        List<Node> list = new ArrayList<Node>();

        int activeSteps = 0;
        for (FlowStep step : flowSteps) {
            if (step.getComponent().getBoolean(AbstractComponentRuntime.ENABLED, true)) {
                activeSteps++;
            }
        }

        // If the execution steps don't match the flow steps, wait and try
        // again.
        // The execution log steps may not be persisted yet.
        for (int i = 0; i < 5; i++) {
            if (executionData.steps.size() == activeSteps) {
                break;
            } else {
                AppUtils.sleep(200);
                executionData = getExecutionData();
            }
        }

        for (FlowStep flowStep : flowSteps) {
            Node node = new Node();
            String name = flowStep.getComponent().getName();
            String type = flowStep.getComponent().getType();
            boolean enabled = flowStep.getComponent().getBoolean(AbstractComponentRuntime.ENABLED, true);
            String imageText = String.format(
                    "<img style=\"display: block; margin-left: auto; margin-right: auto\" src=\"data:image/png;base64,%s\"/>",
                    UiUtils.getBase64RepresentationOfImageForComponentType(type, context));

            node.setText(imageText);
            node.setName(name);
            node.setEnabled(enabled);
            node.setId(flowStep.getId());
            node.setX(flowStep.getX());
            node.setY(flowStep.getY());

            ExecutionStep executionStep = executionData.findExecutionStep(flowStep.getId());
            if (node.isEnabled() && executionStep != null) {
                node.setEntitiesProcessed(executionStep.getEntitiesProcessed());
                node.setMessagesRecieved(executionStep.getMessagesReceived());
                node.setMessagesSent(executionStep.getMessagesProduced());
                node.setStatus(executionStep.getExecutionStatus().toString());
                node.setInputLabel(Long.toString(executionStep.getMessagesReceived()));
                node.setOutputLabel(Long.toString(executionStep.getMessagesProduced()));
            } else if (!node.isEnabled()) {
                node.setInputLabel("-");
                node.setOutputLabel("-");
            } else {
                // Show the detail screen if the flow does not match the
                // historical execution.
                showDetails();
                showDiagram.setEnabled(false);
                showDiagram.setDescription("The flow has been modified since the execution. The flow cannot be viewed.");
            }

            for (FlowStepLink link : links) {
                if (link.getSourceStepId().equals(node.getId())) {
                    node.getTargetNodeIds().add(link.getTargetStepId());
                }
            }

            list.add(node);

        }
        return list;
    }

    protected void logTableCellClicked(Grid logTable, ItemClickEvent event) {
        if (event.isDoubleClick()) {
            Object object = event.getPropertyId();
            if (!object.toString().equals("")) {
                Object prop = event.getPropertyId();
                String header = logTable.getColumn(prop).getHeaderCaption();
                Property<?> p = event.getItem().getItemProperty(prop);
                if (p != null) {
                    String data = String.valueOf(p.getValue());
                    new ReadOnlyTextAreaDialog(header, data, false).showAtSize(.5);
                }
            }
        }
    }

    @Override
    public boolean closing() {
        context.getBackgroundRefresherService().unregister(this);
        return true;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    protected void rerun() {
        parentTabSheet.closeTab(executionId);
        flowRunnable.runFlow();
    }

    protected void remove() {
        ConfirmDialog.show("Delete Execution?", "Are you sure you want to delete this execution?", () -> {
            context.getExecutionService().deleteExecution(executionId);
            parentTabSheet.closeTab(executionId);
            return true;
        });

    }

    protected void cancel() {
        ConfirmDialog.show("Cancel Execution?", "Are you sure you want to cancel this execution?", () -> {
            context.getAgentManager().cancel(executionId);
            cancelButton.setEnabled(false);
            return true;
        });
    }

    protected void showDiagram() {
        showDiagram.setVisible(false);
        showDetails.setVisible(true);
        splitPanel.setFirstComponent(flowPanel);
        Setting setting = context.getUser().findSetting(UserSetting.SETTING_SHOW_RUN_DIAGRAM);
        setting.setValue("true");
        context.getConfigurationService().save(setting);
    }

    protected void showDetails() {
        showDiagram.setVisible(true);
        showDetails.setVisible(false);
        splitPanel.setFirstComponent(stepTable);
        Setting setting = context.getUser().findSetting(UserSetting.SETTING_SHOW_RUN_DIAGRAM);
        setting.setValue("false");
        context.getConfigurationService().save(setting);

    }

    @SuppressWarnings("unchecked")
    @Override
    public Object onBackgroundDataRefresh() {
        if (!lastDataRefreshWasDone) {
            return getExecutionData();
        } else {
            return null;
        }
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        if (backgroundData != null) {
            refreshUI((ExecutionData) backgroundData);
        }
    }

    @SuppressWarnings("unchecked")
    protected ExecutionData getExecutionData() {
        ExecutionData data = new ExecutionData();
        data.execution = executionService.findExecution(executionId);
        data.steps = executionService.findExecutionSteps(executionId);
        this.flow = context.getConfigurationService().findFlow(data.execution.getFlowId());

        Set<String> selected = (Set<String>) stepTable.getValue();
        data.logs = executionService.findExecutionStepLogs(selected);
        return data;
    }

    class RunDiagramChangedListener implements Listener {
        private static final long serialVersionUID = 1L;

        @Override
        public void componentEvent(Event e) {
            if (e instanceof NodeSelectedEvent) {

                NodeSelectedEvent event = (NodeSelectedEvent) e;
                List<String> nodeIds = event.getNodeIds();
                ExecutionData data = getExecutionData();
                Set<String> stepIds = new HashSet<String>(nodeIds.size());

                for (String id : nodeIds) {
                    ExecutionStep step = data.findExecutionStep(id);
                    if (step != null) {
                        stepIds.add(step.getId());
                    }
                }

                logContainer.removeAllItems();
                List<ExecutionStepLog> logs = executionService.findExecutionStepLogs(stepIds);
                logContainer.addAll(logs);
            }
        }
    }

    protected boolean isDone() {
        return ExecutionStatus.isDone(statusLabel.getValue());
    }

    protected void refreshUI(ExecutionData data) {
        if (!lastDataRefreshWasDone) {
            flowLabel.setValue(data.execution.getFlowName());
            startLabel.setValue(formatDate(data.execution.getStartTime()));
            if (data.execution.getStatus() != null) {
                if (data.execution.getStatus().equals(ExecutionStatus.ERROR.name())) {
                    statusLabel.setStyleName("error");
                    statusLabel.setValue(FontAwesome.WARNING.getHtml() + " " + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.DONE.name())) {
                    statusLabel.setStyleName("done");
                    statusLabel.setValue(FontAwesome.CHECK.getHtml() + " " + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.RUNNING.name())) {
                    statusLabel.setStyleName("running");
                    statusLabel.setValue(FontAwesome.SPINNER.getHtml() + " " + data.execution.getStatus());
                } else {
                    statusLabel.setStyleName("");
                    statusLabel.setValue(data.execution.getStatus());
                }
            }
            endLabel.setValue(formatDate(data.execution.getEndTime()));

            // TODO: Be smarter about when we redraw the flow.
            // If we are looking at details, don't waste time updating the flow
            // view.
            redrawFlow();

            @SuppressWarnings("unchecked")
            Set<String> selected = (Set<String>) stepTable.getValue();
            stepContainer.removeAllItems();
            stepContainer.addAll(data.steps);
            if (selected != null && selected.size() > 0) {
                stepTable.setValue(selected);
            } else if (data.steps.size() > 0) {
                selected = new HashSet<>();
                selected.add(data.steps.get(0).getId());
                stepTable.setValue(selected);
            }

            List<ExecutionStepLog> logMessages = new ArrayList<>(logContainer.getItemIds());

            List<ExecutionStepLog> newLogMessages = new ArrayList<>(data.logs);

            for (ExecutionStepLog logMsg : logMessages) {
                newLogMessages.remove(logMsg);
            }

            if (newLogMessages.size() > 0) {
                logContainer.addAll(newLogMessages);
                if (lastSortOrder != null) {
                    logTable.setSortOrder(lastSortOrder);
                }
            }
        }

        rerunButton.setVisible(isDone() && flowRunnable != null);
        removeButton.setVisible(isDone());
        cancelButton.setVisible(!isDone());

        lastDataRefreshWasDone = isDone();
    }

    protected String formatDate(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aa");
        if (date != null) {
            return df.format(date);
        }
        return "";
    }

    public class ExecutionData {
        public Execution execution;
        public List<ExecutionStep> steps;
        public List<ExecutionStepLog> logs;

        ExecutionStep findExecutionStep(String id) {
            ExecutionStep executionStep = null;
            for (ExecutionStep s : steps) {
                if (s.getFlowStepId().equals(id)) {
                    executionStep = s;
                    break;
                }
            }
            return executionStep;
        }
    }

    public class ComponentNameColumnGenerator implements ColumnGenerator {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        public Object generateCell(com.vaadin.ui.Table source, Object itemId, Object columnId) {
            BeanItem<ExecutionStepLog> logItem = (BeanItem<ExecutionStepLog>) source.getItem(itemId);
            String executionStepId = (String) logItem.getItemProperty("executionStepId").getValue();
            BeanItem<ExecutionStep> stepItem = stepContainer.getItem(executionStepId);
            return new Label((String) stepItem.getItemProperty("componentName").getValue());
        }
    }

}
