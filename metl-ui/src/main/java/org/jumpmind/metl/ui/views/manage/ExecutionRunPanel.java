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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.jumpmind.metl.ui.common.IFlowRunnable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.metl.ui.diagram.Node;
import org.jumpmind.metl.ui.diagram.NodeSelectedEvent;
import org.jumpmind.metl.ui.diagram.RunDiagram;
import org.jumpmind.util.AppUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ReadOnlyTextAreaDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.provider.GridSortOrder;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;

import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.components.grid.HeaderRow;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.dnd.DropTargetExtension;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.themes.ValoTheme;

public class ExecutionRunPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable<Object> {

    private static final long serialVersionUID = 1L;

    final protected Logger log = LoggerFactory.getLogger(getClass());

    IExecutionService executionService;

    VerticalSplitPanel splitPanel;
    
    List<ExecutionStep> stepList = new ArrayList<ExecutionStep>();

    Grid<ExecutionStep> stepTable = new Grid<ExecutionStep>();
    
    TextField componentNameFilterField;

    RunDiagram diagram;

    Panel flowPanel;

    AbstractLayout diagramLayout;

    Flow flow;

    List<AbstractObject> selected = new ArrayList<AbstractObject>();

    List<ExecutionStepLog> logList = new ArrayList<ExecutionStepLog>();

    Grid<ExecutionStepLog> logTable;
    
    TextField messageFilterField;
    
    ComboBox<String> levelFilterCombo;

    Label flowLabel = new Label();

    Label statusLabel = new Label();

    Label startLabel = new Label();

    Button downloadLink;

    Label endLabel = new Label();

    Button removeButton;

    Button cancelButton;

    Button rerunButton;

    CheckBox showDiagramCheckbox;

    TextField limitField;

    String executionId;

    ApplicationContext context;

    TabbedPanel parentTabSheet;

    IFlowRunnable flowRunnable;

    boolean lastDataRefreshWasDone = false;

    List<GridSortOrder<ExecutionStepLog>> lastSortOrder;

    Label status;
    
    float lastPosition = 50;
    
    // Must be set to 99. Not sure why 100 doesn't work.
    final static float MAX_PANEL_POSITION = 99;

    public ExecutionRunPanel(String executionId, ApplicationContext context,
            TabbedPanel parentTabSheet) {
        this(executionId, context, parentTabSheet, null);
    }

    @SuppressWarnings("unchecked")
	public ExecutionRunPanel(String executionId, ApplicationContext context,
            TabbedPanel parentTabSheet, IFlowRunnable flowRunnable) {
        this.executionService = context.getExecutionService();
        this.executionId = executionId;
        this.context = context;
        this.parentTabSheet = parentTabSheet;
        this.flowRunnable = flowRunnable;

        Execution execution = executionService.findExecution(executionId);
        this.flow = context.getConfigurationService().findFlow(execution.getFlowId());

        HorizontalLayout topBar = new HorizontalLayout();
        topBar.setMargin(new MarginInfo(true, true, false, true));
        topBar.setWidth(100, Unit.PERCENTAGE);

        HorizontalLayout left = new HorizontalLayout();
        topBar.addComponent(left);

        HorizontalLayout right = new HorizontalLayout();
        right.setSpacing(true);
        topBar.addComponent(right);
        topBar.setComponentAlignment(right, Alignment.MIDDLE_RIGHT);

        Label limitLabel = new Label("Max Log Messages To Show :");
        right.addComponent(limitLabel);
        right.setComponentAlignment(limitLabel, Alignment.MIDDLE_RIGHT);
        limitField = new TextField();
        limitField.setValueChangeMode(ValueChangeMode.LAZY);
        limitField.setValueChangeTimeout(200);
        limitField.addValueChangeListener(event -> {
            Setting setting = context.getUser()
                    .findSetting(UserSetting.SETTING_MAX_LOG_MESSAGE_TO_SHOW);
            setting.setValue(Integer.toString(getMaxToShow(event.getValue())));
            context.getConfigurationService().save(setting);
        });
        limitField.setWidth("5em");
        limitField.setValue(
                context.getUser().get(UserSetting.SETTING_MAX_LOG_MESSAGE_TO_SHOW, "1000"));
        right.addComponent(limitField);
        right.setComponentAlignment(limitField, Alignment.MIDDLE_RIGHT);

        showDiagramCheckbox = new CheckBox("Show Diagram");
        showDiagramCheckbox.addValueChangeListener((event) -> {
            if (showDiagramCheckbox.getValue()) {
                showDiagram();
            } else {
                showDetails();
            }
        });
        right.addComponent(showDiagramCheckbox);
        right.setComponentAlignment(showDiagramCheckbox, Alignment.MIDDLE_RIGHT);

        addComponent(topBar);

        ButtonBar buttonBar = new ButtonBar();

        rerunButton = buttonBar.addButton("Rerun", Icons.RUN, event -> rerun());
        rerunButton.setVisible(false);
        removeButton = buttonBar.addButton("Remove", Icons.DELETE, event -> remove());
        removeButton.setVisible(false);
        cancelButton = buttonBar.addButton("Cancel", Icons.CANCEL, event -> cancel());

        addComponent(buttonBar);

        HorizontalLayout header1 = new HorizontalLayout();
        Label flowTitleLabel = new Label("<b>Flow:</b>");
        flowTitleLabel.setContentMode(ContentMode.HTML);
        header1.addComponent(flowTitleLabel);
        header1.addComponent(flowLabel);
        Label startTitleLabel = new Label("<b>Start:</b>");
        startTitleLabel.setContentMode(ContentMode.HTML);
        header1.addComponent(startTitleLabel);
        header1.addComponent(startLabel);
        header1.setSpacing(true);
        header1.setMargin(new MarginInfo(false, true, false, true));
        header1.setWidth("100%");
        addComponent(header1);

        HorizontalLayout header2 = new HorizontalLayout();
        Label statusTitleLabel = new Label("<b>Status:</b>");
        statusTitleLabel.setContentMode(ContentMode.HTML);
        header2.addComponent(statusTitleLabel);
        statusLabel.setContentMode(ContentMode.HTML);
        header2.addComponent(statusLabel);
        Label endTitleLabel = new Label("<b>End:</b>");
        endTitleLabel.setContentMode(ContentMode.HTML);
        header2.addComponent(endTitleLabel);
        header2.addComponent(endLabel);
        header2.setSpacing(true);
        header2.setMargin(new MarginInfo(false, true, true, true));
        header2.setWidth("100%");
        addComponent(header2);

        diagramLayout = new VerticalLayout();
        diagramLayout.setWidth(10000, Unit.PIXELS);
        diagramLayout.setHeight(10000, Unit.PIXELS);

        flowPanel = new Panel();
        flowPanel.setSizeFull();
        flowPanel.addStyleName(ValoTheme.PANEL_WELL);
        
        new DropTargetExtension<AbstractLayout>(diagramLayout);
        flowPanel.setContent(diagramLayout);

        stepTable.setSelectionMode(SelectionMode.SINGLE);
        stepTable.setSizeFull();
        stepTable.addColumn(ExecutionStep::getComponentName).setId("componentName").setCaption("Component Name").setWidth(250);
        stepTable.addColumn(ExecutionStep::getThreadNumber).setCaption("Thread").setWidth(100);
        stepTable.addColumn(ExecutionStep::getStatus).setCaption("Status").setWidth(120);
        stepTable.addColumn(ExecutionStep::getPayloadReceived).setCaption("Payload Recvd").setWidth(120);
        stepTable.addColumn(ExecutionStep::getMessagesReceived).setCaption("Msgs Recvd").setWidth(100);
        stepTable.addColumn(ExecutionStep::getMessagesProduced).setCaption("Msgs Sent").setWidth(100);
        stepTable.addColumn(ExecutionStep::getPayloadProduced).setCaption("Payload Send").setWidth(120);
		stepTable.addColumn(ExecutionStep::getStartTime).setCaption("Start").setWidth(120).setMaximumWidth(170)
				.setRenderer(new DateRenderer(UIConstants.TIME_FORMAT));
		stepTable.addColumn(ExecutionStep::getEndTime).setCaption("End").setWidth(120).setMaximumWidth(170)
				.setRenderer(new DateRenderer(UIConstants.TIME_FORMAT));
		stepTable.addColumn(ExecutionStep::getHandleDurationString).setCaption("Run Duration").setWidth(140);
		stepTable.addColumn(ExecutionStep::getQueueDurationString).setCaption("Wait Duration").setWidth(140);
        stepTable.addSelectionListener(event -> {
            ExecutionStep selected = ((SingleSelectionModel<ExecutionStep>) stepTable).getSelectedItem().orElse(null);
            String stepId = selected != null ? selected.getId() : null;
            logList = executionService.findExecutionStepLogs(stepId, getMaxToShow());
            refreshLogTable();
            downloadLink.setVisible(logList.size() > 0);
            setLogMinimized(logTable.getDataProvider().size(new Query<>())==0);
            updateStatus();
        });
        
        componentNameFilterField = new TextField();
        componentNameFilterField.setPlaceholder("Filter");
        componentNameFilterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        componentNameFilterField.setWidth("100%");
        componentNameFilterField.setValueChangeMode(ValueChangeMode.LAZY);
        componentNameFilterField.addValueChangeListener(change -> refreshStepTable());
        HeaderRow stepTableFilterHeader = stepTable.appendHeaderRow();
        stepTableFilterHeader.getCell("componentId").setComponent(componentNameFilterField);

        logTable = new Grid<ExecutionStepLog>();
        logTable.addColumn(ExecutionStepLog::getLevel).setId("Level").setCaption("Level").setWidth(110).setMaximumWidth(200);
		logTable.addColumn(ExecutionStepLog::getCreateTime).setId("Time").setCaption("Time").setWidth(120).setMaximumWidth(200)
				.setRenderer(new DateRenderer(UIConstants.TIME_FORMAT));
		logTable.addColumn(ExecutionStepLog::getLogText).setId("Message").setCaption("Message").setExpandRatio(1);
        logTable.setSizeFull();
        logTable.addItemClickListener(event -> logTableCellClicked(logTable, event));
        logTable.addSortListener(event -> {
            lastSortOrder = event.getSortOrder();
        });

        
        messageFilterField = new TextField();
        messageFilterField.setPlaceholder("Filter");
        messageFilterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        messageFilterField.setWidth("100%");

        // Update filter When the filter input is changed
        messageFilterField.addValueChangeListener(change -> refreshLogTable());
        HeaderRow filteringHeader = logTable.appendHeaderRow();
        filteringHeader.getCell("Message").setComponent(messageFilterField);

        levelFilterCombo = new ComboBox<String>();
        levelFilterCombo.setWidth(8, Unit.EM);
        levelFilterCombo.setEmptySelectionAllowed(true);
        List<String> itemList = new ArrayList<String>();
        LogLevel[] levels = LogLevel.values();
        for (LogLevel logLevel : levels) {
            itemList.add(logLevel.name());
        }
        levelFilterCombo.setItems(itemList);
        levelFilterCombo.addValueChangeListener(change -> refreshLogTable());
        filteringHeader.getCell("Level").setComponent(levelFilterCombo);

        levelFilterCombo.addStyleName(ValoTheme.COMBOBOX_TINY);

        VerticalLayout logLayout = new VerticalLayout();
        logLayout.setSizeFull();
        logLayout.addComponent(logTable);
        logLayout.setExpandRatio(logTable, 1);

        HorizontalLayout statusBar = new HorizontalLayout();
        statusBar.addStyleName(ValoTheme.PANEL_WELL);
        statusBar.setMargin(new MarginInfo(true, true, true, true));
        statusBar.setWidth(100, Unit.PERCENTAGE);

        status = new Label("", ContentMode.HTML);
        statusBar.addComponent(status);
        statusBar.setComponentAlignment(status, Alignment.MIDDLE_LEFT);
        logLayout.addComponent(statusBar);

        downloadLink = new Button("Download", VaadinIcons.DOWNLOAD);
        downloadLink.addClickListener(e -> download());
        downloadLink.addStyleName(ValoTheme.BUTTON_LINK);
        statusBar.addComponent(downloadLink);
        statusBar.setComponentAlignment(downloadLink, Alignment.MIDDLE_RIGHT);

        splitPanel = new VerticalSplitPanel();
        splitPanel.setFirstComponent(flowPanel);
        splitPanel.setSecondComponent(logLayout);
        splitPanel.setSplitPosition(50, Unit.PERCENTAGE);
        splitPanel.setSizeFull();
        addComponent(splitPanel);
        setExpandRatio(splitPanel, 1.0f);

        showDiagramCheckbox
                .setValue(context.getUser().getBoolean(UserSetting.SETTING_SHOW_RUN_DIAGRAM, true));
        if (!showDiagramCheckbox.getValue()) {
            showDetails();
        }

        context.getBackgroundRefresherService().register(this);
    }

    @SuppressWarnings("unchecked")
	protected void download() {
        String stepId = null;
        if (showDiagramCheckbox.getValue()) {
            if (diagram.getSelectedNodeIds().size()>0) {
                String flowStepId = diagram.getSelectedNodeIds().get(0);
                ExecutionData data = getExecutionData();
                if (data != null) {
                    ExecutionStep executionStep = data.findExecutionStep(flowStepId);
                    if (executionStep != null) {
                        stepId = executionStep.getId();
                    }
                }
            }
        } else {
            ExecutionStep selected = ((SingleSelectionModel<ExecutionStep>) stepTable).getSelectedItem().orElse(null);
            stepId = selected != null ? selected.getId() : null;
        }
        
        if (stepId != null) {
            final File file = executionService.getExecutionStepLog(stepId);
            StreamSource ss = new StreamSource() {
                private static final long serialVersionUID = 1L;

                public InputStream getStream() {
                    try {
                        return new FileInputStream(file);
                    } catch (Exception e) {
                        log.error("Failed to download log file", e);
                        CommonUiUtils.notify("Failed to download log file", Type.ERROR_MESSAGE);
                        return null;
                    }
                }
            };
            StreamResource resource = new StreamResource(ss, file.getName());
            final String KEY = "export";
            setResource(KEY, resource);
            Page.getCurrent().open(ResourceReference.create(resource, this, KEY).getURL(), null);
        }
    }

    protected void redrawFlow() {
        if (diagram != null) {
            diagramLayout.removeComponent(diagram);
        }
        diagram = new RunDiagram();
        diagram.setSizeFull();
        diagram.addListener(new RunDiagramChangedListener());
        diagram.setNodes(getNodes());
        setLogMinimized(diagram.getSelectedNodeIds().size()==0);
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
        // again.  The execution log steps may not be persisted yet.
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
            boolean enabled = flowStep.getComponent().getBoolean(AbstractComponentRuntime.ENABLED,
                    true);
            String imageText = String.format(
                    "<img style=\"display: block; margin-left: auto; margin-right: auto\" src=\"data:image/png;base64,%s\"/>",
                    UiUtils.getBase64RepresentationOfImageForComponentType(flow.getProjectVersionId(), type, context));

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
                showDiagramCheckbox.setEnabled(false);
                showDiagramCheckbox.setDescription(
                        "The flow has been modified since the execution. The flow cannot be viewed.");
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

    protected void logTableCellClicked(Grid<ExecutionStepLog> logTable, ItemClick<ExecutionStepLog> event) {
        if (event.getMouseEventDetails().isDoubleClick()) {
        	Column<ExecutionStepLog, ?> column = event.getColumn();
        	ExecutionStepLog log = event.getItem();
        	if (column != null && log != null) {
        		String data = String.valueOf(column.getValueProvider().apply(log));
        		String header = column.getId();
        		new ReadOnlyTextAreaDialog(header, data, false).showAtSize(.5);
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
        ConfirmDialog.show("Delete Execution?", "Are you sure you want to delete this execution?",
                () -> {
                    context.getExecutionService().deleteExecution(executionId);
                    parentTabSheet.closeTab(executionId);
                    return true;
                });

    }

    protected void cancel() {
        ConfirmDialog.show("Cancel Execution?", "Are you sure you want to cancel this execution?",
                () -> {
                    context.getAgentManager().cancel(executionId);
                    cancelButton.setEnabled(false);
                    return true;
                });
    }

    protected void showDiagram() {
        splitPanel.setFirstComponent(flowPanel);
        Setting setting = context.getUser().findSetting(UserSetting.SETTING_SHOW_RUN_DIAGRAM);
        setting.setValue("true");
        context.getConfigurationService().save(setting);
        redrawFlow();
    }

    protected void showDetails() {
        splitPanel.setFirstComponent(stepTable);
        setLogMinimized(stepTable.getSelectedItems().isEmpty());
        Setting setting = context.getUser().findSetting(UserSetting.SETTING_SHOW_RUN_DIAGRAM);
        setting.setValue("false");
        context.getConfigurationService().save(setting);
    }

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
    
    public void onUIError(Throwable ex) {
        CommonUiUtils.notify(ex);   
    }

    @SuppressWarnings("unchecked")
	protected ExecutionData getExecutionData() {
        ExecutionData data = new ExecutionData();
        data.execution = executionService.findExecution(executionId);
        data.steps = executionService.findExecutionSteps(executionId);
        this.flow = context.getConfigurationService().findFlow(data.execution.getFlowId());

        ExecutionStep selected = ((SingleSelectionModel<ExecutionStep>) stepTable).getSelectedItem().orElse(null);
        String selectedId = selected != null ? selected.getId() : null;
        data.logs = executionService.findExecutionStepLogs(selectedId, getMaxToShow());
        return data;
    }

    protected int getMaxToShow() {
        return getMaxToShow(limitField.getValue());
    }

    protected int getMaxToShow(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return 100;
        }
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

                logList = executionService.findExecutionStepLogs(stepIds, getMaxToShow());
                refreshLogTable();
                setLogMinimized(logList.size()==0);
                updateStatus();
            }
        }
    }
    
    protected void setLogMinimized(boolean minimize) {
        float position = splitPanel.getSplitPosition();
        if (minimize && position != MAX_PANEL_POSITION) {
            lastPosition = position;
            splitPanel.setSplitPosition(MAX_PANEL_POSITION, Unit.PERCENTAGE);
        } else if (!minimize && position == MAX_PANEL_POSITION) {
            splitPanel.setSplitPosition(lastPosition, Unit.PERCENTAGE);
        }
    }

    protected void updateStatus() {
    	int logCount = logTable.getDataProvider().size(new Query<>());
        boolean max = logCount >= getMaxToShow();
        if (max) {
            status.setValue(
                    "<span style='color:red'>Displaying only " + logCount
                            + " messages.  Adjust max number of log message to show more.</span>");
        } else {
            status.setValue(
                    "<span>Displaying " + logCount + " messages</span>");
        }
    }

    protected boolean isDone() {
        boolean done = ExecutionStatus.isDone(statusLabel.getValue());
        if (done) {
            List<ExecutionStep> steps = stepTable.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
            for (ExecutionStep step : steps) {
                if (!ExecutionStatus.isDone(step.getStatus())) {
                    done = false;
                    break;
                }
            }
        }
        return done;
    }

    @SuppressWarnings("unchecked")
    protected void refreshUI(ExecutionData data) {
        if (!lastDataRefreshWasDone) {
            flowLabel.setValue(data.execution.getFlowName());
            startLabel.setValue(formatDate(data.execution.getStartTime()));
            if (data.execution.getStatus() != null) {
                if (data.execution.getStatus().equals(ExecutionStatus.ERROR.name())) {
                    statusLabel.setStyleName("error");
                    statusLabel.setValue(
                    		VaadinIcons.WARNING.getHtml() + " " + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.DONE.name())) {
                    statusLabel.setStyleName("done");
                    statusLabel.setValue(
                    		VaadinIcons.CHECK.getHtml() + " " + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.RUNNING.name())) {
                    statusLabel.setStyleName("running");
                    statusLabel.setValue(
                    		VaadinIcons.SPINNER.getHtml() + " " + data.execution.getStatus());
                } else {
                    statusLabel.setStyleName("");
                    statusLabel.setValue(data.execution.getStatus());
                }
            }
            endLabel.setValue(formatDate(data.execution.getEndTime()));

            if (showDiagramCheckbox.getValue()) {
                redrawFlow();
            }

            ExecutionStep selected = ((SingleSelectionModel<ExecutionStep>) stepTable).getSelectedItem().orElse(null);
            
            stepList = data.steps;
            refreshStepTable();

            if (selected == null && data.steps.size() > 0) {
                stepTable.deselectAll();
            }

            List<ExecutionStepLog> newLogMessages = new ArrayList<>(data.logs);

            for (ExecutionStepLog logMsg : logList) {
                newLogMessages.remove(logMsg);
            }

            if (newLogMessages.size() > 0) {
                logList.addAll(newLogMessages);
                refreshLogTable();
                if (lastSortOrder != null) {
                    logTable.setSortOrder(lastSortOrder);
                }
                updateStatus();
            }
            
            lastDataRefreshWasDone = isDone();
            
            rerunButton.setVisible(lastDataRefreshWasDone && flowRunnable != null);
            removeButton.setVisible(lastDataRefreshWasDone);
            cancelButton.setVisible(!lastDataRefreshWasDone);
        }
    }
    
    protected void refreshStepTable() {
    	List<ExecutionStep> filteredStepList = new ArrayList<ExecutionStep>();
    	String filterValue = componentNameFilterField.getValue();
    	if (isNotBlank(filterValue)) {
        	for (ExecutionStep step : stepList) {
        		String componentName = step.getComponentName();
        		if (componentName != null && componentName.toLowerCase().contains(filterValue.toLowerCase())) {
        			filteredStepList.add(step);
        		}
        	}
    	} else {
    		filteredStepList.addAll(stepList);
    	}
    	stepTable.setItems(filteredStepList);
    }
    
    protected void refreshLogTable() {
    	List<ExecutionStepLog> filteredLogList = new ArrayList<ExecutionStepLog>();
    	String messageFilterValue = messageFilterField.getValue();
    	String levelFilterValue = levelFilterCombo.getValue();
    	if (isNotBlank(messageFilterValue) || isNotBlank(levelFilterValue)) {
    		for (ExecutionStepLog log : logList) {
    			String message = log.getLogText();
    			String level = log.getLevel();
				if ((isBlank(messageFilterValue) || (message != null && message.toLowerCase().contains(messageFilterValue.toLowerCase())))
						&& (isBlank(levelFilterValue) || (level != null && level.toLowerCase().contains(levelFilterValue.toLowerCase())))) {
    				filteredLogList.add(log);
    			}
    		}
    	} else {
    		filteredLogList.addAll(logList);
    	}
    	logTable.setItems(filteredLogList);
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

}
