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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.ExecutionStep;
import org.jumpmind.metl.core.model.ExecutionStepLog;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.Table;
import org.jumpmind.metl.ui.views.IFlowRunnable;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ReadOnlyTextAreaDialog;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

@SuppressWarnings("serial")
public class ExecutionLogPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable {

    IExecutionService executionService;

    Table stepTable = new Table();

    BeanContainer<String, ExecutionStep> stepContainer = new BeanContainer<String, ExecutionStep>(
            ExecutionStep.class);

    BeanItemContainer<ExecutionStepLog> logContainer = new BeanItemContainer<ExecutionStepLog>(
            ExecutionStepLog.class);

    Label flowLabel = new Label();

    Label statusLabel = new Label("", ContentMode.HTML);

    Label startLabel = new Label();

    Label endLabel = new Label();
    
    Button removeButton;
    
    Button cancelButton;
    
    Button rerunButton;

    String executionId;
    
    ApplicationContext context;
    
    TabbedPanel parentTabSheet;
    
    IFlowRunnable flowRunnable;

    public ExecutionLogPanel(String executionId, ApplicationContext context, TabbedPanel parentTabSheet, IFlowRunnable flowRunnable) {
        this.executionService = context.getExecutionService();
        this.executionId = executionId;
        this.context = context;
        this.parentTabSheet = parentTabSheet;
        this.flowRunnable = flowRunnable;
        
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
        stepTable.setContainerDataSource(stepContainer);
        stepTable.setSelectable(true);
        stepTable.setMultiSelect(true);
        stepTable.setImmediate(true);
        stepTable.setSizeFull();
        stepTable.setVisibleColumns(new Object[] { "componentName", "threadNumber", "status", "messagesReceived",
                "messagesProduced", "entitiesProcessed", "startTime", "endTime" });
        stepTable.setColumnHeaders(new String[] { "Component Name", "Thread", "Status", "Msgs Recvd",
                "Msgs Sent", "Entities Prcd", "Start", "End" });
        stepTable.setColumnWidth("status", 100);
        stepTable.setColumnWidth("messagesReceived", 100);
        stepTable.setColumnWidth("messagesProduced", 100);
        stepTable.setColumnWidth("entitiesProcessed", 100);
        stepTable.setColumnWidth("threadNumber", 100);
        stepTable.setColumnWidth("startTime", 100);
        stepTable.setColumnExpandRatio("endTime", 1);
        stepTable.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                @SuppressWarnings("unchecked")
                Set<String> executionStepIds = (Set<String>) event.getProperty().getValue();
                logContainer.removeAllItems();
                List<ExecutionStepLog> logs = executionService
                        .findExecutionStepLog(executionStepIds);
                logContainer.addAll(logs);
            }
        });

        final Table logTable = new Table();
        logTable.setContainerDataSource(logContainer);
        logTable.setSelectable(true);
        logTable.setMultiSelect(true);
        logTable.setSizeFull();
        logTable.addGeneratedColumn("componentName", new ComponentNameColumnGenerator());
        logTable.setVisibleColumns(new Object[] { "componentName", "level", "createTime", "logText" });
        logTable.setColumnHeaders(new String[] { "Component Name", "Level", "Time", "Description" });
        logTable.addItemClickListener(new ItemClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    Object object = event.getPropertyId();
                    if (!object.toString().equals("")) {
                        Object prop = event.getPropertyId();
                        String header = logTable.getColumnHeader(prop);
                        Property<?> p = event.getItem().getItemProperty(prop);
                        if (p != null) {
                            String data = String.valueOf(p.getValue());
                            new ReadOnlyTextAreaDialog(header, data, false).showAtSize(.5);
                        }
                    }
                }
            }
        });

        VerticalSplitPanel splitPanel = new VerticalSplitPanel();
        splitPanel.setFirstComponent(stepTable);
        splitPanel.setSecondComponent(logTable);
        splitPanel.setSplitPosition(50f);
        splitPanel.setSizeFull();
        addComponent(splitPanel);
        setExpandRatio(splitPanel, 1.0f);

        context.getBackgroundRefresherService().register(this);        
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
        ConfirmDialog.show("Delete Execution?",
                "Are you sure you want to delete this execution?",
                new IConfirmListener() {
                    
                    @Override
                    public boolean onOk() {
                        context.getExecutionService().deleteExecution(executionId);
                        parentTabSheet.closeTab(executionId);
                        return true;
                    }
                });

    }
    
    protected void cancel() {
        ConfirmDialog.show("Cancel Execution?",
                "Are you sure you want to cancel this execution?",
                new IConfirmListener() {
                    
                    @Override
                    public boolean onOk() {
                        context.getAgentManager().cancel(executionId);
                        cancelButton.setEnabled(false);
                        return true;
                    }
                });
        
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object onBackgroundDataRefresh() {
        return getExecutionData();
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        refreshUI((ExecutionData) backgroundData);
    }

    @SuppressWarnings("unchecked")
    protected ExecutionData getExecutionData() {
        ExecutionData data = new ExecutionData();
        data.execution = executionService.findExecution(executionId);
        data.steps = executionService.findExecutionSteps(executionId);
        data.logs = executionService.findExecutionStepLog((Set<String>) stepTable.getValue());
        return data;
    }
    
    protected boolean isDone() {
        return ExecutionStatus.isDone(statusLabel.getValue());
    }

    protected void refreshUI(ExecutionData data) {
        if (!isDone()) {
            flowLabel.setValue(data.execution.getFlowName());
            startLabel.setValue(formatDate(data.execution.getStartTime()));
            if (data.execution.getStatus() != null) {
                if (data.execution.getStatus().equals(ExecutionStatus.ERROR.name())) {
                    statusLabel.setStyleName("error");
                    statusLabel.setValue(FontAwesome.WARNING.getHtml() + " "
                            + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.DONE.name())) {
                    statusLabel.setStyleName("done");
                    statusLabel.setValue(FontAwesome.CHECK.getHtml() + " "
                            + data.execution.getStatus());
                } else if (data.execution.getStatus().equals(ExecutionStatus.RUNNING.name())) {
                    statusLabel.setStyleName("running");
                    statusLabel.setValue(FontAwesome.SPINNER.getHtml() + " "
                            + data.execution.getStatus());
                } else {
                    statusLabel.setStyleName("");
                    statusLabel.setValue(data.execution.getStatus());
                }
            }
            endLabel.setValue(formatDate(data.execution.getEndTime()));

            stepContainer.removeAllItems();
            stepContainer.addAll(data.steps);

            List<ExecutionStepLog> logMessages = new ArrayList<ExecutionStepLog>(
                    logContainer.getItemIds());

            for (ExecutionStepLog logMsg : data.logs) {
                logMessages.remove(logMsg);
            }

            if (logMessages.size() > 0) {
                logContainer.removeAllItems();
                logContainer.addAll(data.logs);
            }
        }
        
        rerunButton.setVisible(isDone() && flowRunnable != null);
        removeButton.setVisible(isDone());
        cancelButton.setVisible(!isDone());
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
    }

    public class ComponentNameColumnGenerator implements ColumnGenerator {
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
