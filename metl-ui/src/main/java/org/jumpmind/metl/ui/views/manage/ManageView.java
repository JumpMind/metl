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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.AgentName;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.IBackgroundRefreshable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.metl.ui.common.View;
import org.jumpmind.vaadin.ui.common.ColumnVisibilityToggler;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.Manage, name = "Manage", id = "manage", icon = VaadinIcon.COGS, menuOrder = 25)
@Route("manage")
public class ManageView extends HorizontalLayout implements BeforeEnterObserver, IUiPanel, IBackgroundRefreshable<Object>, View {
    
    final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ANY = "<Any>";

    private static final long serialVersionUID = 1L;

    static final int DEFAULT_LIMIT = 100;

    //@Autowired
    ApplicationContext context;

    ManageNavigator manageNavigator;

    TabbedPanel tabs;

    List<Execution> executionList = new ArrayList<Execution>();

    Grid<Execution> grid;

    Button viewButton;

    ComboBox<String> statusSelect;
    
    TextField filterField;

    int limit = DEFAULT_LIMIT;

    @SuppressWarnings("serial")
    public ManageView(@Autowired ApplicationContext context) {
        this.context = context;
        viewButton = new Button("View Log");
        viewButton.setEnabled(false);
        viewButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            public void onComponentEvent(ClickEvent<Button> event) {
                viewLog(grid.getSelectionModel().getFirstSelectedItem().orElse(null));
            }
        });

        VerticalLayout mainTab = new VerticalLayout();
        mainTab.setSizeFull();
        HorizontalLayout header = new HorizontalLayout();
        header.setDefaultVerticalComponentAlignment(Alignment.END);
        Span spacer = new Span();
        header.add(spacer, viewButton);
        header.expand(spacer);

        statusSelect = new ComboBox<String>("Status");
        List<String> statusList = new ArrayList<String>();
        statusList.add(ANY);
        for (ExecutionStatus status : ExecutionStatus.values()) {
            statusList.add(status.toString());
        }
        statusSelect.setItems(statusList);
        statusSelect.setValue(ANY);
        statusSelect.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            @Override
            public void valueChanged(ValueChangeEvent<String> event) {
                if (event.getValue() != null) {
                    refreshUI(getBackgroundData(), true);
                } else {
                    statusSelect.setValue(event.getOldValue());
                }
            }
        });
        header.add(statusSelect);

        HorizontalLayout limitLayout = new HorizontalLayout();
        limitLayout.setSpacing(true);
        Span limitSpan = new Span("Limit:");
        Span limitSpacer = new Span();
        limitLayout.add(limitSpacer, limitSpan);
        limitLayout.expand(limitSpacer);
        limitLayout.setVerticalComponentAlignment(Alignment.CENTER, limitSpan);
        TextField limitField = new TextField(null, String.valueOf(DEFAULT_LIMIT));
        limitField.setWidth("5em");
        limitField.setValueChangeMode(ValueChangeMode.LAZY);
        limitField.setValueChangeTimeout(200);
        limitField.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            public void valueChanged(ValueChangeEvent<String> event) {
                try {
                    limit = Integer.parseInt(event.getValue());
                } catch (Exception e) {
                }
                refreshUI(getBackgroundData(), true);
            }
        });
        limitLayout.add(limitField);
        limitLayout.setVerticalComponentAlignment(Alignment.END, limitField);
        header.add(limitLayout);
        header.expand(limitLayout);

        filterField = new TextField();
        filterField.setPlaceholder("Filter");
        filterField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(200);
        filterField.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            public void valueChanged(ValueChangeEvent<String> event) {
                refreshGrid();
            }
        });
        header.add(filterField);

        header.setSpacing(true);
        header.setMargin(true);
        header.setWidth("100%");
        mainTab.add(header);

        grid = new Grid<Execution>();
        grid.setSizeFull();
        grid.addItemClickListener(new ComponentEventListener<ItemClickEvent<Execution>>() {
            @Override
            public void onComponentEvent(ItemClickEvent<Execution> event) {
                if (event.getClickCount() == 2) {
                    viewLog(event.getItem());
                }
            }
        });
        ColumnVisibilityToggler columnVisibilityToggler = new ColumnVisibilityToggler();
        mainTab.add(columnVisibilityToggler);
        mainTab.setHorizontalComponentAlignment(Alignment.END, columnVisibilityToggler);
        columnVisibilityToggler.addColumn(grid.addColumn(Execution::getAgentName).setHeader("Agent").setWidth("250px"), "Agent");
        columnVisibilityToggler.addColumn(grid.addColumn(Execution::getDeploymentName).setHeader("Deployment").setWidth("250px"), "Deployment");
        columnVisibilityToggler.addColumn(grid.addColumn(Execution::getHostName).setHeader("Host").setWidth("145px"), "Host").setVisible(false);
        columnVisibilityToggler.addColumn(grid.addColumn(Execution::getStatus).setHeader("Status").setWidth("90px"), "Status");
        columnVisibilityToggler.addColumn(grid.addColumn(item -> CommonUiUtils.formatDateTime(item.getStartTime())).setKey("startTime")
                .setSortProperty("startTime").setHeader("Start").setWidth("170px"), "Start");
        columnVisibilityToggler.addColumn(grid.addColumn(item -> CommonUiUtils.formatDateTime(item.getEndTime())).setSortProperty("endTime")
                .setHeader("End").setWidth("170px"), "End");
        columnVisibilityToggler.addColumn(grid.addColumn(Execution::getCreateBy).setHeader("Caller").setWidth("100px"), "Caller");
        columnVisibilityToggler.addColumn(grid.addColumn(Execution::getParameters).setHeader("Parameters").setWidth("5000px"), "Parameters");
        for (Column<Execution> column : grid.getColumns()) {
            column.setSortable(true);
        }
        List<GridSortOrder<Execution>> orderList = new ArrayList<GridSortOrder<Execution>>();
        orderList.add(new GridSortOrder<Execution>(grid.getColumnByKey("startTime"), SortDirection.DESCENDING));
        grid.sort(orderList);
        grid.addSelectionListener((event) -> viewButton.setEnabled(event.getFirstSelectedItem().isPresent()));
        mainTab.addAndExpand(grid);

        tabs = new TabbedPanel();
        tabs.setMainTab("Executions", new Icon(Icons.EXECUTION), mainTab);

        SplitLayout split = new SplitLayout();
        split.setSizeFull();
        split.setSplitterPosition(20);

        manageNavigator = new ManageNavigator(FolderType.AGENT, context);
        manageNavigator.addValueChangeListener((event) -> {
            Object currentSelection = manageNavigator.getCurrentSelection();
            if (currentSelection != null) {
                if (statusSelect.isReadOnly()) {
                    statusSelect.setReadOnly(false);
                    statusSelect.setValue(ANY);
                }
                if (currentSelection.equals(ManageNavigator.CURRENTLY_RUNNING)) {
                    statusSelect.setValue(ExecutionStatus.RUNNING.name());
                    statusSelect.setReadOnly(true);
                } else if (currentSelection.equals(ManageNavigator.IN_ERROR)) {
                    statusSelect.setValue(ExecutionStatus.ERROR.name());
                    statusSelect.setReadOnly(true);
                }
            }
            refreshUI(getBackgroundData(), true);
        });
        split.addToPrimary(manageNavigator);

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.add(tabs);
        split.addToSecondary(container);

        add(split);
        setSizeFull();
        context.getBackgroundRefresherService().register(this);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        manageNavigator.refresh();
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

    @Override
    public Object onBackgroundDataRefresh() {
        return getBackgroundData();
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        refreshUI(backgroundData, false);
    }

    @Override
    public void onUIError(Throwable ex) {
        log.error("", ex);
        CommonUiUtils.notifyError();        
    }
    
    public Object getBackgroundData() {
        Object currentSelection = manageNavigator.getCurrentSelection();
        Object currentSelectionParent = manageNavigator.getCurrentSelectionParent();
        if (currentSelection != null) {
            Map<String, Object> params = new HashMap<String, Object>();
            if (!currentSelection.equals(ManageNavigator.CURRENTLY_RUNNING) && !currentSelection.equals(ManageNavigator.IN_ERROR)) {
                if (currentSelection instanceof Agent) {
                    params.put("agentId", ((Agent) currentSelection).getId());
                } else if (currentSelection instanceof AgentName) {
                    params.put("agentId", ((AgentName) currentSelection).getId());
                } else if (currentSelection instanceof FlowName) {
                    params.put("flowId", ((FlowName) currentSelection).getId());
                } else if (currentSelection instanceof AgentDeploy) {
                    params.put("deploymentId", ((AgentDeploy) currentSelection).getId());
                } else if (currentSelection instanceof AgentDeploymentSummary) {
                    params.put("deploymentId", ((AgentDeploymentSummary) currentSelection).getId());
                }
            }

            if (currentSelectionParent instanceof Agent) {
                params.put("agentId", ((Agent) currentSelectionParent).getId());
            } else if (currentSelectionParent instanceof AgentName) {
                params.put("agentId", ((AgentName) currentSelectionParent).getId());
            }

            if (!statusSelect.getValue().equals(ANY)) {
                params.put("status", statusSelect.getValue());
            }

            if (params.size() > 0) {
                return context.getExecutionService().findExecutions(params, limit);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void refreshUI(Object obj, boolean tabToFront) {
        List<Execution> data = (List<Execution>) obj;
        if (needsUpdated(data)) {
            Execution currentGridSelection = grid.getSelectionModel().getFirstSelectedItem().orElse(null);
            grid.deselectAll();
            executionList.clear();
            if (data != null) {
                executionList.addAll(data);
            }
            refreshGrid();
            if (data != null && currentGridSelection != null) {
                grid.select(currentGridSelection);
            }
            viewButton.setEnabled(data != null && currentGridSelection != null);
            if (tabToFront) {
                tabs.mainTabToTop();
            }
        }
    }
    
    protected void refreshGrid() {
        List<Execution> filteredExecutionList = new ArrayList<Execution>();
        String filterValue = filterField.getValue();
        if (StringUtils.isNotBlank(filterValue)) {
            for (Execution execution : executionList) {
                if (StringUtils.containsIgnoreCase(execution.getAgentName(), filterValue)
                        && StringUtils.containsIgnoreCase(execution.getHostName(), filterValue)
                        && StringUtils.containsIgnoreCase(execution.getFlowName(), filterValue)
                        && StringUtils.containsIgnoreCase(execution.getStatus(), filterValue)
                        && StringUtils.containsIgnoreCase(CommonUiUtils.formatDateTime(execution.getStartTime()), filterValue)
                        && StringUtils.containsIgnoreCase(CommonUiUtils.formatDateTime(execution.getEndTime()), filterValue)) {
                    filteredExecutionList.add(execution);
                }
            }
        } else {
            filteredExecutionList.addAll(executionList);
        }
        grid.setItems(filteredExecutionList);
    }

    protected boolean needsUpdated(List<Execution> data) {
        boolean needsUpdated = false;
        List<Execution> all = data != null ? new ArrayList<Execution>(data)
                : new ArrayList<Execution>(0);

        if (all.size() != executionList.size()) {
            log.debug("new execution count = " + all.size() + ", old execution count = " + executionList.size());
            needsUpdated = true;
        }
        
        if (!needsUpdated) {
            for (Execution execution : executionList) {
                if (execution.getExecutionStatus() == ExecutionStatus.RUNNING) {
                    for (Execution newExecution : all) {
                        if (newExecution.equals(execution) &&  
                                newExecution.getExecutionStatus() != execution.getExecutionStatus()) {
                            needsUpdated = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!needsUpdated) {
            all.removeAll(executionList);
            log.debug("different execution count = " + all.size());
            needsUpdated = all.size() > 0;
        }

        return needsUpdated;
    }

    protected void viewLog(Object item) {
        if (item != null) {
            Execution execution = (Execution) item;
            ExecutionRunPanel logPanel = new ExecutionRunPanel(execution.getId(), context, tabs, null);
            tabs.addCloseableTab(execution.getId(), "Log " + execution.getFlowName(), new Icon(Icons.LOG), logPanel);
            logPanel.onBackgroundUIRefresh(logPanel.onBackgroundDataRefresh());
        }
    }

}
