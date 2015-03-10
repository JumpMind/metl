package org.jumpmind.symmetric.is.ui.views.manage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

public class ExecutionLogPanel extends VerticalLayout {

	private static final long serialVersionUID = 1L;
	
	protected IExecutionService executionService;
	
	protected FlowVersion flowVersion;
	
	protected BeanContainer<String, ExecutionStep> stepContainer;
	
	protected BeanItemContainer<ExecutionStepLog> logContainer;

	public ExecutionLogPanel(IExecutionService executionService, FlowVersion flowVersion) {
		this.executionService = executionService;
		this.flowVersion = flowVersion;

		Execution execution = queryForExecution(flowVersion);
		HorizontalLayout header1 = new HorizontalLayout();
		header1.addComponent(new Label("<b>Flow:</b>", ContentMode.HTML));
		header1.addComponent(new Label(flowVersion.getName()));
		header1.addComponent(new Label("<b>Start:</b>", ContentMode.HTML));
		header1.addComponent(new Label(formatDate(execution.getStartTime())));
		header1.setSpacing(true);
		header1.setMargin(new MarginInfo(true, true, false, true));
		header1.setWidth("100%");
		addComponent(header1);
		
		HorizontalLayout header2 = new HorizontalLayout();
		header2.addComponent(new Label("<b>Status:</b>", ContentMode.HTML));
		header2.addComponent(new Label(execution.getStatus()));
		header2.addComponent(new Label("<b>End:</b>", ContentMode.HTML));
		header2.addComponent(new Label(formatDate(execution.getEndTime())));
		header2.setSpacing(true);
		header2.setMargin(new MarginInfo(false, true, true, true));
		header2.setWidth("100%");
		addComponent(header2);
		
		ArrayList<ExecutionStep> steps = queryForSteps();
		stepContainer = new BeanContainer<String, ExecutionStep>(ExecutionStep.class);
		stepContainer.setBeanIdProperty("id");
		stepContainer.addAll(steps);

		Table stepTable = new Table();
		stepTable.setContainerDataSource(stepContainer);
		stepTable.setSelectable(true);
		stepTable.setMultiSelect(true);
		stepTable.setImmediate(true);
		stepTable.setSizeFull();
		stepTable.setVisibleColumns(new Object[] { "componentName", "status", "messagesReceived", "messagesProduced", "startTime", "endTime" });
		stepTable.setColumnHeaders(new String[] { "Component Name", "Status", "Msgs Recvd", "Msgs Sent", "Start", "End" });
		stepTable.addValueChangeListener(new ValueChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void valueChange(ValueChangeEvent event) {
				@SuppressWarnings("unchecked")
				Set<String> executionStepIds = (Set<String>) event.getProperty().getValue();
				logContainer.removeAllItems();
				for (String executionStepId : executionStepIds) {
					ArrayList<ExecutionStepLog> logs = queryForLogs(executionStepId);
					logContainer.addAll(logs);
				}
			}			
		});

		logContainer = new BeanItemContainer<ExecutionStepLog>(ExecutionStepLog.class);

		Table logTable = new Table();
		logTable.setContainerDataSource(logContainer);
		logTable.setSelectable(true);
		logTable.setMultiSelect(true);
		logTable.setSizeFull();
		logTable.addGeneratedColumn("componentName", new ComponentNameColumnGenerator());
		logTable.setVisibleColumns(new Object[] { "componentName", "category", "level", "createTime", "logText" });
		logTable.setColumnHeaders(new String[] { "Component Name", "Category", "Level", "Time", "Description" });

		VerticalSplitPanel splitPanel = new VerticalSplitPanel();
		splitPanel.setFirstComponent(stepTable);
		splitPanel.setSecondComponent(logTable);
		splitPanel.setSplitPosition(50f);
		splitPanel.setSizeFull();
		addComponent(splitPanel);
		setExpandRatio(splitPanel, 1.0f);
	}
	
	protected Execution queryForExecution(FlowVersion flowVersion) {
		Execution e = new Execution();
		e.setId("1");
		e.setCreateBy("elong");
		e.setCreateTime(new Date());
		e.setStartTime(new Date());
		e.setStatus("RUNNING");
		return e;
	}

	protected ArrayList<ExecutionStep> queryForSteps() {
		ArrayList<ExecutionStep> steps = new ArrayList<ExecutionStep>();

		ExecutionStep e = new ExecutionStep();
		e.setId("123");
		e.setComponentName("FileReader");
		e.setStatus("RUNNING");
		e.setMessagesReceived(2);
		e.setMessagesProduced(1);
		e.setStartTime(new Date());
		steps.add(e);

		e = new ExecutionStep();
		e.setId("124");
		e.setComponentName("Cruncher");
		e.setStatus("RUNNING");
		e.setMessagesReceived(1);
		e.setMessagesProduced(1);
		e.setStartTime(new Date());
		steps.add(e);

		return steps;
	}
	
	protected ArrayList<ExecutionStepLog> queryForLogs(String executionStepId) {
		ArrayList<ExecutionStepLog> logs = new ArrayList<ExecutionStepLog>();
		ExecutionStepLog e = new ExecutionStepLog();
		if (executionStepId.equals("123")) {
			e.setId("321");
			e.setExecutionStepId("123");
			e.setCategory("Integration");
			e.setLevel("INFO");
			e.setCreateTime(new Date());
			e.setLogText("Opening file for reading");
			logs.add(e);
	
			e = new ExecutionStepLog();
			e.setId("322");
			e.setExecutionStepId("123");
			e.setCategory("Integration");
			e.setLevel("INFO");
			e.setCreateTime(new Date());
			e.setLogText("Parsing file");
			logs.add(e);
	
			e = new ExecutionStepLog();
			e.setId("323");
			e.setExecutionStepId("123");
			e.setCategory("Integration");
			e.setLevel("INFO");
			e.setCreateTime(new Date());
			e.setLogText("Processing");
			logs.add(e);
		}
		
		if (executionStepId.equals("124")) {
			e = new ExecutionStepLog();
			e.setId("324");
			e.setExecutionStepId("124");
			e.setCategory("Subsystem");
			e.setLevel("WARN");
			e.setCreateTime(new Date());
			e.setLogText("Cruncher is hungry");
			logs.add(e);
	
			e = new ExecutionStepLog();
			e.setId("325");
			e.setExecutionStepId("124");
			e.setCategory("Subwoofer");
			e.setLevel("DEBUG");
			e.setCreateTime(new Date());
			e.setLogText("Cruncher angry now");
			logs.add(e);
		}

		return logs;
	}

	protected String formatDate(Date date) {
		SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aa");
		if (date != null) {
			return df.format(date);
		}
		return "";
	}
    public class ComponentNameColumnGenerator implements ColumnGenerator {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        public Object generateCell(Table source, Object itemId, Object columnId) {
            BeanItem<ExecutionStepLog> logItem = (BeanItem<ExecutionStepLog>) source.getItem(itemId);
            String executionStepId = (String) logItem.getItemProperty("executionStepId").getValue();
            BeanItem<ExecutionStep> stepItem = stepContainer.getItem(executionStepId);
            return new Label((String) stepItem.getItemProperty("componentName").getValue());
        }
    }

}
