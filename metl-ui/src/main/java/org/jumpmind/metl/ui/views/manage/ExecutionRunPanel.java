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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.jumpmind.vaadin.ui.common.CustomSplitLayout;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ReadOnlyTextAreaDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;

public class ExecutionRunPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable<Object> {

    private static final long serialVersionUID = 1L;

    final protected Logger log = LoggerFactory.getLogger(getClass());

    IExecutionService executionService;

    CustomSplitLayout splitPanel;
    
    List<ExecutionStep> stepList = new ArrayList<ExecutionStep>();

    Grid<ExecutionStep> stepGrid = new Grid<ExecutionStep>();
    
    TextField componentNameFilterField;

    RunDiagram diagram;

    Div flowDiv;

    VerticalLayout diagramLayout;

    Flow flow;

    List<AbstractObject> selected = new ArrayList<AbstractObject>();

    List<ExecutionStepLog> logList = new ArrayList<ExecutionStepLog>();

    Grid<ExecutionStepLog> logGrid;
    
    Map<String, ValueProvider<ExecutionStepLog, String>> logValueProviderMap = new HashMap<String, ValueProvider<ExecutionStepLog, String>>();
    
    VerticalLayout logLayout;
    
    TextField messageFilterField;
    
    ComboBox<String> levelFilterCombo;

    Span flowSpan = new Span();

    HorizontalLayout statusLabel = new HorizontalLayout();
    
    String executionStatus;

    Span startSpan = new Span();

    Anchor downloadAnchor;

    Button downloadLink;
    
    boolean generateNewLogFile = true;

    Span endSpan = new Span();

    Button removeButton;

    Button cancelButton;

    Button rerunButton;

    Checkbox showDiagramCheckbox;

    TextField limitField;

    String executionId;

    ApplicationContext context;

    TabbedPanel parentTabSheet;

    IFlowRunnable flowRunnable;

    boolean lastDataRefreshWasDone = false;

    List<GridSortOrder<ExecutionStepLog>> lastSortOrder;

    Span status;
    
    double lastPosition = 50;
    
    // Must be set to 99. Not sure why 100 doesn't work.
    final static float MAX_PANEL_POSITION = 99;

    public ExecutionRunPanel(String executionId, ApplicationContext context,
            TabbedPanel parentTabSheet) {
        this(executionId, context, parentTabSheet, null);
    }

	public ExecutionRunPanel(String executionId, ApplicationContext context,
            TabbedPanel parentTabSheet, IFlowRunnable flowRunnable) {
        this.executionService = context.getExecutionService();
        this.executionId = executionId;
        this.context = context;
        this.parentTabSheet = parentTabSheet;
        this.flowRunnable = flowRunnable;
        
        setPadding(false);
        setSpacing(false);

        Execution execution = executionService.findExecution(executionId);
        this.flow = context.getConfigurationService().findFlow(execution.getFlowId());

        HorizontalLayout topBar = new HorizontalLayout();
        //topBar.getStyle().set("margin", "16px 16px 0 16px");
        topBar.setWidthFull();

        HorizontalLayout left = new HorizontalLayout();
        topBar.add(left);

        topBar.addAndExpand(new Span());

        HorizontalLayout right = new HorizontalLayout();
        right.setSpacing(true);
        topBar.add(right);
        topBar.setVerticalComponentAlignment(Alignment.CENTER, right);

        Span spacer = new Span();
        right.add(spacer);
        right.expand(spacer);

        Span limitSpan = new Span("Max Log Messages To Show :");
        right.add(limitSpan);
        right.setVerticalComponentAlignment(Alignment.CENTER, limitSpan);
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
        right.add(limitField);
        right.setVerticalComponentAlignment(Alignment.CENTER, limitField);

        showDiagramCheckbox = new Checkbox("Show Diagram");
        showDiagramCheckbox.addValueChangeListener((event) -> {
            if (showDiagramCheckbox.getValue()) {
                showDiagram();
            } else {
                showDetails();
            }
        });
        right.add(showDiagramCheckbox);
        right.setVerticalComponentAlignment(Alignment.CENTER, showDiagramCheckbox);

        add(topBar);

        ButtonBar buttonBar = new ButtonBar();

        rerunButton = buttonBar.addButton("Rerun", Icons.RUN, event -> rerun());
        rerunButton.setVisible(false);
        removeButton = buttonBar.addButton("Remove", Icons.DELETE, event -> remove());
        removeButton.setVisible(false);
        cancelButton = buttonBar.addButton("Cancel", Icons.CANCEL, event -> cancel());

        add(buttonBar);
        
        HorizontalLayout header = new HorizontalLayout(
                createHeaderLayout(new Html("<b>Flow:</b>"), new Html("<b>Status:</b>")),
                createHeaderLayout(flowSpan, statusLabel),
                createHeaderLayout(new Html("<b>Start:</b>"), new Html("<b>End:</b>")),
                createHeaderLayout(startSpan, endSpan));
        header.setWidthFull();
        header.getStyle().set("padding-left", "16px");
        add(header);

        diagramLayout = new VerticalLayout();
        diagramLayout.setWidth("10000px");
        diagramLayout.setHeight("10000px");

        flowDiv = new Div();
        flowDiv.setSizeFull();
        
        DropTarget.create(diagramLayout);
        flowDiv.add(diagramLayout);

        stepGrid.setSelectionMode(SelectionMode.SINGLE);
        stepGrid.setSizeFull();
        stepGrid.addColumn(ExecutionStep::getComponentName).setKey("componentName").setHeader("Component Name")
                .setFlexGrow(0).setWidth("250px");
        stepGrid.addColumn(ExecutionStep::getThreadNumber).setHeader("Thread").setFlexGrow(0).setWidth("100px");
        stepGrid.addColumn(ExecutionStep::getStatus).setHeader("Status").setFlexGrow(0).setWidth("120px");
        stepGrid.addColumn(ExecutionStep::getPayloadReceived).setHeader("Payload Recvd").setFlexGrow(0).setWidth("130px");
        stepGrid.addColumn(ExecutionStep::getMessagesReceived).setHeader("Msgs Recvd").setFlexGrow(0).setWidth("110px");
        stepGrid.addColumn(ExecutionStep::getMessagesProduced).setHeader("Msgs Sent").setFlexGrow(0).setWidth("100px");
        stepGrid.addColumn(ExecutionStep::getPayloadProduced).setHeader("Payload Sent").setFlexGrow(0).setWidth("120px");
		stepGrid.addColumn(step -> {
		    Date startTime = step.getStartTime();
		    if (startTime != null) {
		        return String.format(UIConstants.TIME_FORMAT, startTime);
		    }
		    return "";
		}).setHeader("Start").setFlexGrow(0).setWidth("120px");
		stepGrid.addColumn(step -> {
            Date endTime = step.getEndTime();
            if (endTime != null) {
                return String.format(UIConstants.TIME_FORMAT, endTime);
            }
            return "";
        }).setHeader("End").setFlexGrow(0).setWidth("120px");
		stepGrid.addColumn(ExecutionStep::getHandleDurationString).setHeader("Run Duration").setFlexGrow(0).setWidth("140px");
		stepGrid.addColumn(ExecutionStep::getQueueDurationString).setHeader("Wait Duration").setFlexGrow(0).setWidth("140px");
        stepGrid.addSelectionListener(event -> {
            ExecutionStep selected = stepGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
            String stepId = selected != null ? selected.getId() : null;
            logList = executionService.findExecutionStepLogs(stepId, getMaxToShow());
            refreshLogTable();
            downloadLink.setVisible(logList.size() > 0);
            downloadAnchor.setVisible(logList.size() > 0);
            setLogMinimized(logGrid.getDataProvider().size(new Query<>())==0);
            updateStatus();
        });
        
        componentNameFilterField = new TextField();
        componentNameFilterField.setPlaceholder("Filter");
        componentNameFilterField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        componentNameFilterField.setWidth("100%");
        componentNameFilterField.setValueChangeMode(ValueChangeMode.LAZY);
        componentNameFilterField.addValueChangeListener(change -> refreshStepTable());
        HeaderRow stepTableFilterHeader = stepGrid.appendHeaderRow();
        stepTableFilterHeader.getCell(stepGrid.getColumnByKey("componentName")).setComponent(componentNameFilterField);

        logGrid = new Grid<ExecutionStepLog>();
        logGrid.addColumn(ExecutionStepLog::getLevel).setKey("Level").setHeader("Level").setFlexGrow(0).setWidth("160px");
        logValueProviderMap.put("Level", ExecutionStepLog::getLevel);
        ValueProvider<ExecutionStepLog, String> timeValueProvider = log -> {
            Date createTime = log.getCreateTime();
            if (createTime != null) {
                return String.format(UIConstants.TIME_FORMAT, createTime);
            }
            return "";
        };
		logGrid.addColumn(timeValueProvider).setKey("Time").setHeader("Time").setFlexGrow(0).setWidth("120px");
		logValueProviderMap.put("Time", timeValueProvider);
		logGrid.addColumn(ExecutionStepLog::getLogText).setKey("Message").setHeader("Message").setFlexGrow(1);
		logValueProviderMap.put("Message", ExecutionStepLog::getLogText);
        logGrid.setSizeFull();
        logGrid.addItemClickListener(event -> logGridCellClicked(event));
        logGrid.addSortListener(event -> {
            lastSortOrder = event.getSortOrder();
        });

        
        messageFilterField = new TextField();
        messageFilterField.setPlaceholder("Filter");
        messageFilterField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        messageFilterField.setWidth("100%");

        // Update filter When the filter input is changed
        messageFilterField.addValueChangeListener(change -> refreshLogTable());
        HeaderRow filteringHeader = logGrid.appendHeaderRow();
        filteringHeader.getCell(logGrid.getColumnByKey("Message")).setComponent(messageFilterField);

        levelFilterCombo = new ComboBox<String>();
        levelFilterCombo.setWidth("8em");
        List<String> itemList = new ArrayList<String>();
        LogLevel[] levels = LogLevel.values();
        for (LogLevel logLevel : levels) {
            itemList.add(logLevel.name());
        }
        levelFilterCombo.setItems(itemList);
        levelFilterCombo.addValueChangeListener(change -> refreshLogTable());
        filteringHeader.getCell(logGrid.getColumnByKey("Level")).setComponent(levelFilterCombo);

        logLayout = new VerticalLayout();
        logLayout.setSpacing(false);
        logLayout.setSizeFull();
        logLayout.addAndExpand(logGrid);

        HorizontalLayout statusBar = new HorizontalLayout();
        statusBar.setMargin(true);
        statusBar.setWidthFull();

        status = new Span("");
        statusBar.add(status);
        statusBar.setVerticalComponentAlignment(Alignment.CENTER, status);
        logLayout.add(statusBar);

        statusBar.addAndExpand(new Span());

        downloadAnchor = new Anchor();
        downloadAnchor.setTarget("_blank");
        downloadAnchor.getElement().setAttribute("download", true);
        downloadLink = new Button("Download", new Icon(VaadinIcon.DOWNLOAD));
        downloadLink.addClickListener(e -> download());
        downloadLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        downloadAnchor.add(downloadLink);
        statusBar.add(downloadAnchor);
        statusBar.setVerticalComponentAlignment(Alignment.CENTER, downloadLink);

        splitPanel = new CustomSplitLayout();
        splitPanel.setOrientation(Orientation.VERTICAL);
        splitPanel.addToPrimary(flowDiv);
        splitPanel.addToSecondary(logLayout);
        splitPanel.setSplitterPosition(50);
        splitPanel.setSizeFull();
        add(splitPanel);
        expand(splitPanel);

        showDiagramCheckbox
                .setValue(context.getUser().getBoolean(UserSetting.SETTING_SHOW_RUN_DIAGRAM, true));
        if (!showDiagramCheckbox.getValue()) {
            showDetails();
        }

        context.getBackgroundRefresherService().register(this);
    }
	
	private VerticalLayout createHeaderLayout(Component topComponent, Component bottomComponent) {
	    VerticalLayout headerLayout = new VerticalLayout(topComponent, bottomComponent);
	    headerLayout.setWidth("25%");
	    headerLayout.setPadding(false);
	    headerLayout.setSpacing(false);
	    return headerLayout;
	}

	protected void download() {
        if (generateNewLogFile) {
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
                ExecutionStep selected = stepGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
                stepId = selected != null ? selected.getId() : null;
            }
            
            if (stepId != null) {
                final File file = executionService.getExecutionStepLog(stepId);
                InputStreamFactory factory = new InputStreamFactory() {
                    private static final long serialVersionUID = 1L;

                    public InputStream createInputStream() {
                        try {
                            return new FileInputStream(file);
                        } catch (Exception e) {
                            log.error("Failed to download log file", e);
                            CommonUiUtils.notifyError("Failed to download log file");
                            return null;
                        }
                    }
                };
                StreamResource resource = new StreamResource(file.getName(), factory);
                downloadAnchor.setHref(resource);
                generateNewLogFile = false;
                UI.getCurrent().getPage().executeJs("$0.click();", downloadAnchor.getElement());
            }
        } else {
            downloadAnchor.removeHref();
            generateNewLogFile = true;
        }
    }

    protected void redrawFlow() {
        if (diagram != null) {
            diagramLayout.remove(diagram);
        }
        diagram = new RunDiagram(this);
        diagram.setSizeFull();
        diagram.setNodes(getNodes());
        setLogMinimized(diagram.getSelectedNodeIds().size()==0);
        diagramLayout.add(diagram);
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
                showDiagramCheckbox.getElement().setProperty("title",
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

    protected void logGridCellClicked(ItemClickEvent<ExecutionStepLog> event) {
        if (event.getClickCount() == 2) {
        	Column<ExecutionStepLog> column = event.getColumn();
        	ExecutionStepLog log = event.getItem();
        	if (column != null && log != null) {
        	    String header = column.getKey();
        		String data = logValueProviderMap.get(header).apply(log);
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
        new ConfirmDialog("Delete Execution?", "Are you sure you want to delete this execution?", "Ok", event -> {
            context.getExecutionService().deleteExecution(executionId);
            parentTabSheet.closeTab(executionId);
        }).open();
    }

    protected void cancel() {
        new ConfirmDialog("Cancel Execution?", "Are you sure you want to cancel this execution?", "Ok", event -> {
            context.getAgentManager().cancel(executionId);
            cancelButton.setEnabled(false);
        }).open();
    }

    protected void showDiagram() {
        splitPanel.removeAll();
        splitPanel.addToPrimary(flowDiv);
        splitPanel.addToSecondary(logLayout);
        Setting setting = context.getUser().findSetting(UserSetting.SETTING_SHOW_RUN_DIAGRAM);
        setting.setValue("true");
        context.getConfigurationService().save(setting);
        redrawFlow();
    }

    protected void showDetails() {
        splitPanel.removeAll();
        splitPanel.addToPrimary(stepGrid);
        splitPanel.addToSecondary(logLayout);
        setLogMinimized(stepGrid.getSelectedItems().isEmpty());
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
        log.error("", ex);
        CommonUiUtils.notifyError();   
    }

	protected ExecutionData getExecutionData() {
        ExecutionData data = new ExecutionData();
        data.execution = executionService.findExecution(executionId);
        data.steps = executionService.findExecutionSteps(executionId);
        this.flow = context.getConfigurationService().findFlow(data.execution.getFlowId());

        ExecutionStep selected = stepGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
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
    
    public void nodeSelectedEvent(NodeSelectedEvent event) {
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
    
    protected void setLogMinimized(boolean minimize) {
        double position = splitPanel.getSplitterPosition();
        if (minimize && position != MAX_PANEL_POSITION) {
            lastPosition = position;
            splitPanel.setSplitterPosition(MAX_PANEL_POSITION);
        } else if (!minimize && position == MAX_PANEL_POSITION) {
            splitPanel.setSplitterPosition(lastPosition);
        }
    }

    protected void updateStatus() {
    	int logCount = logGrid.getDataProvider().size(new Query<>());
        boolean max = logCount >= getMaxToShow();
        if (max) {
            status.getStyle().set("color", "red");
            status.setText(
                    "Displaying only " + logCount + " messages.  Adjust max number of log message to show more.");
        } else {
            status.getStyle().remove("color");
            status.setText("Displaying " + logCount + " messages");
        }
    }

    protected boolean isDone() {
        boolean done = ExecutionStatus.isDone(executionStatus);
        if (done) {
            List<ExecutionStep> steps = stepGrid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
            for (ExecutionStep step : steps) {
                if (!ExecutionStatus.isDone(step.getStatus())) {
                    done = false;
                    break;
                }
            }
        }
        return done;
    }

    protected void refreshUI(ExecutionData data) {
        if (!lastDataRefreshWasDone) {
            flowSpan.setText(data.execution.getFlowName());
            startSpan.setText(formatDate(data.execution.getStartTime()));
            executionStatus = data.execution.getStatus();
            if (executionStatus != null) {
                statusLabel.removeAll();
                if (executionStatus.equals(ExecutionStatus.ERROR.name())) {
                    statusLabel.setClassName("error");
                    statusLabel.add(new Icon(VaadinIcon.WARNING), new Span(executionStatus));
                } else if (executionStatus.equals(ExecutionStatus.DONE.name())) {
                    statusLabel.setClassName("done");
                    Icon icon = new Icon(VaadinIcon.CHECK);
                    icon.setColor("green");
                    statusLabel.add(icon, new Span(executionStatus));
                } else if (executionStatus.equals(ExecutionStatus.RUNNING.name())) {
                    statusLabel.setClassName("running");
                    statusLabel.add(new Icon(VaadinIcon.SPINNER), new Span(executionStatus));
                } else {
                    statusLabel.setClassName("");
                    statusLabel.add(new Span(executionStatus));
                }
            }
            endSpan.setText(formatDate(data.execution.getEndTime()));

            if (showDiagramCheckbox.getValue()) {
                redrawFlow();
            }

            ExecutionStep selected = stepGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
            
            stepList = data.steps;
            refreshStepTable();

            if (selected == null && data.steps.size() > 0) {
                stepGrid.deselectAll();
            }

            List<ExecutionStepLog> newLogMessages = new ArrayList<>(data.logs);

            for (ExecutionStepLog logMsg : logList) {
                newLogMessages.remove(logMsg);
            }

            if (newLogMessages.size() > 0) {
                logList.addAll(newLogMessages);
                refreshLogTable();
                if (lastSortOrder != null) {
                    logGrid.sort(lastSortOrder);
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
    	stepGrid.setItems(filteredStepList);
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
    	logGrid.setItems(filteredLogList);
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
