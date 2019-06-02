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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
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
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.themes.ValoTheme;

public class ExecutionRunPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable<Object> {

    private static final long serialVersionUID = 1L;

    final protected Logger log = LoggerFactory.getLogger(getClass());

    IExecutionService executionService;

    VerticalSplitPanel splitPanel;

    Grid stepTable = new Grid();

    RunDiagram diagram;

    Panel flowPanel;

    AbstractLayout diagramLayout;

    Flow flow;

    List<AbstractObject> selected = new ArrayList<AbstractObject>();

    Grid logTable;

    BeanContainer<String, ExecutionStep> stepContainer = new BeanContainer<String, ExecutionStep>(
            ExecutionStep.class);

    BeanItemContainer<ExecutionStepLog> logContainer = new BeanItemContainer<ExecutionStepLog>(
            ExecutionStepLog.class);

    Label flowLabel = new Label();

    Label statusLabel = new Label("", ContentMode.HTML);

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

    List<SortOrder> lastSortOrder;

    Label status;
    
    float lastPosition = 50;
    
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
        limitField = new ImmediateUpdateTextField(null) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void save(String text) {
                Setting setting = context.getUser()
                        .findSetting(UserSetting.SETTING_MAX_LOG_MESSAGE_TO_SHOW);
                setting.setValue(Integer.toString(getMaxToShow(text)));
                context.getConfigurationService().save(setting);
            }
        };
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
        
        // Wrapper fixes issue with the diagram not expanding inside the scroll panel.
        DragAndDropWrapper wrapper = new DragAndDropWrapper(diagramLayout);
        wrapper.setSizeUndefined();
        flowPanel.setContent(wrapper);

        stepTable.setSelectionMode(SelectionMode.SINGLE);
        stepTable.setImmediate(true);
        stepTable.setSizeFull();
        stepTable.addColumn("componentName", String.class).setHeaderCaption("Component Name")
                .setWidth(250);
        stepTable.addColumn("threadNumber", Integer.class).setHeaderCaption("Thread").setWidth(100);
        stepTable.addColumn("status", String.class).setHeaderCaption("Status").setWidth(120);
        stepTable.addColumn("payloadReceived", Integer.class).setHeaderCaption("Payload Recvd")
                .setWidth(120);
        stepTable.addColumn("messagesReceived", Integer.class).setHeaderCaption("Msgs Recvd")
                .setWidth(100);
        stepTable.addColumn("messagesProduced", Integer.class).setHeaderCaption("Msgs Sent")
                .setWidth(100);
        stepTable.addColumn("payloadProduced", Integer.class).setHeaderCaption("Payload Sent")
                .setWidth(120);
        stepTable.addColumn("startTime", Date.class).setHeaderCaption("Start").setWidth(120)
                .setMaximumWidth(170).setRenderer(new DateRenderer(UIConstants.TIME_FORMAT));
        stepTable.addColumn("endTime", Date.class).setHeaderCaption("End").setWidth(120)
                .setMaximumWidth(170).setRenderer(new DateRenderer(UIConstants.TIME_FORMAT));
        stepTable.addColumn("handleDurationString", String.class).setHeaderCaption("Run Duration")
                .setWidth(140);
        stepTable.addColumn("queueDurationString", String.class).setHeaderCaption("Wait Duration")
                .setWidth(140);
        stepTable.setContainerDataSource(stepContainer);
        stepTable.addSelectionListener(event -> {
            String stepId = (String) stepTable.getSelectedRow();
            logContainer.removeAllItems();
            List<ExecutionStepLog> logs = executionService.findExecutionStepLogs(stepId,
                    getMaxToShow());
            logContainer.addAll(logs);
            downloadLink.setVisible(logs.size() > 0);
            setLogMinimized(logContainer.size()==0);
            updateStatus();
        });
        
        HeaderRow stepTableFilterHeader = stepTable.appendHeaderRow();
        HeaderCell componentNameFilterCell = stepTableFilterHeader.getCell("componentName");
        TextField componentNameFilterField = new TextField();
        componentNameFilterField.setInputPrompt("Filter");
        componentNameFilterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
        componentNameFilterField.setWidth("100%");
        componentNameFilterField.addTextChangeListener(change -> {
            stepContainer.removeContainerFilters("componentName");
            if (!change.getText().isEmpty())
                stepContainer.addContainerFilter(
                        new SimpleStringFilter("componentName", change.getText(), true, false));
        });
        componentNameFilterCell.setComponent(componentNameFilterField);


        logTable = new Grid();
        logTable.addColumn("level", String.class).setHeaderCaption("Level").setWidth(110)
                .setMaximumWidth(200);
        logTable.addColumn("createTime", Date.class).setHeaderCaption("Time").setWidth(120)
                .setMaximumWidth(200).setRenderer(new DateRenderer(UIConstants.TIME_FORMAT));
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
                logContainer.addContainerFilter(
                        new SimpleStringFilter("logText", change.getText(), true, false));
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

        downloadLink = new Button("Download", FontAwesome.DOWNLOAD);
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
            stepId = (String) stepTable.getSelectedRow();
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
        setLogMinimized(stepTable.getSelectedRows().isEmpty());
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

    protected ExecutionData getExecutionData() {
        ExecutionData data = new ExecutionData();
        data.execution = executionService.findExecution(executionId);
        data.steps = executionService.findExecutionSteps(executionId);
        this.flow = context.getConfigurationService().findFlow(data.execution.getFlowId());

        String selected = (String) stepTable.getSelectedRow();
        data.logs = executionService.findExecutionStepLogs(selected, getMaxToShow());
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

                logContainer.removeAllItems();
                List<ExecutionStepLog> logs = executionService.findExecutionStepLogs(stepIds,
                        getMaxToShow());
                logContainer.addAll(logs);
                setLogMinimized(logContainer.size()==0);
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
        boolean max = logContainer.getItemIds().size() >= getMaxToShow();
        if (max) {
            status.setValue(
                    "<span style='color:red'>Displaying only " + logContainer.getItemIds().size()
                            + " messages.  Adjust max number of log message to show more.</span>");
        } else {
            status.setValue(
                    "<span>Displaying " + logContainer.getItemIds().size() + " messages</span>");
        }
    }

    protected boolean isDone() {
        boolean done = ExecutionStatus.isDone(statusLabel.getValue());
        if (done) {
            List<String> ids = stepContainer.getItemIds();
            for (String id : ids) {
                ExecutionStep step = stepContainer.getItem(id).getBean();
                if (!ExecutionStatus.isDone(step.getStatus())) {
                    done = false;
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
                            FontAwesome.WARNING.getHtml() + " " + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.DONE.name())) {
                    statusLabel.setStyleName("done");
                    statusLabel.setValue(
                            FontAwesome.CHECK.getHtml() + " " + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.RUNNING.name())) {
                    statusLabel.setStyleName("running");
                    statusLabel.setValue(
                            FontAwesome.SPINNER.getHtml() + " " + data.execution.getStatus());
                } else {
                    statusLabel.setStyleName("");
                    statusLabel.setValue(data.execution.getStatus());
                }
            }
            endLabel.setValue(formatDate(data.execution.getEndTime()));

            if (showDiagramCheckbox.getValue()) {
                redrawFlow();
            }

            String selected = (String) stepTable.getSelectedRow();
            if (stepContainer.size() != data.steps.size()) {
                stepContainer.removeAllItems();
                stepContainer.addAll(data.steps);
            } else {
                for (ExecutionStep step : data.steps) {
                    BeanItem<ExecutionStep> item = stepContainer.getItem(step.getId());
                    item.getItemProperty("status").setValue(step.getStatus());
                    item.getItemProperty("payloadReceived").setValue(step.getPayloadReceived());
                    item.getItemProperty("messagesReceived").setValue(step.getMessagesReceived());
                    item.getItemProperty("messagesProduced").setValue(step.getMessagesProduced());
                    item.getItemProperty("payloadProduced").setValue(step.getPayloadProduced());
                    item.getItemProperty("endTime").setValue(step.getEndTime());
                    item.getItemProperty("startTime").setValue(step.getStartTime());
                    item.getItemProperty("handleDuration").setValue(step.getHandleDuration());
                    item.getItemProperty("queueDuration").setValue(step.getQueueDuration());

                }
            }

            if (selected == null && data.steps.size() > 0) {
                stepTable.select(selected);
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
                updateStatus();
            }
            
            lastDataRefreshWasDone = isDone();
            
            rerunButton.setVisible(lastDataRefreshWasDone && flowRunnable != null);
            removeButton.setVisible(lastDataRefreshWasDone);
            cancelButton.setVisible(!lastDataRefreshWasDone);
        }
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
            BeanItem<ExecutionStepLog> logItem = (BeanItem<ExecutionStepLog>) source
                    .getItem(itemId);
            String executionStepId = (String) logItem.getItemProperty("executionStepId").getValue();
            BeanItem<ExecutionStep> stepItem = stepContainer.getItem(executionStepId);
            return new Label((String) stepItem.getItemProperty("componentName").getValue());
        }
    }

}
