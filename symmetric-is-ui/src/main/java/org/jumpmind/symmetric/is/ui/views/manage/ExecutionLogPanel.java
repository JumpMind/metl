package org.jumpmind.symmetric.is.ui.views.manage;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;
import org.jumpmind.symmetric.is.core.model.FlowVersion;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.VerticalSplitPanel;

public class ExecutionLogPanel extends VerticalSplitPanel {

	private static final long serialVersionUID = 1L;
	
	protected FlowVersion flowVersion;
	
	protected BeanContainer<String, ExecutionStep> stepContainer;
	
	protected BeanItemContainer<ExecutionStepLog> logContainer;

	public ExecutionLogPanel(FlowVersion flowVersion) {
		this.flowVersion = flowVersion;

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

		setFirstComponent(stepTable);
		setSecondComponent(logTable);
		setSplitPosition(50f);
		setSizeFull();
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
