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

import javax.annotation.PostConstruct;

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
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.ItemClickListener;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.Manage, name = "Manage", id = "manage", icon = VaadinIcons.COGS, menuOrder = 25)
public class ManageView extends HorizontalLayout implements View, IUiPanel, IBackgroundRefreshable<Object> {
    
    final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ANY = "<Any>";

    private static final long serialVersionUID = 1L;

    static final int DEFAULT_LIMIT = 100;

    @Autowired
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
    @PostConstruct
    protected void init() {
        viewButton = new Button("View Log");
        viewButton.setEnabled(false);
        viewButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                viewLog(((SingleSelectionModel<Execution>) grid.getSelectionModel()).getSelectedItem().orElse(null));
            }
        });

        VerticalLayout mainTab = new VerticalLayout();
        mainTab.setSizeFull();
        HorizontalLayout header = new HorizontalLayout();
        header.addComponent(viewButton);
        header.setComponentAlignment(viewButton, Alignment.BOTTOM_RIGHT);

        statusSelect = new ComboBox<String>("Status");
        statusSelect.setEmptySelectionAllowed(false);
        List<String> statusList = new ArrayList<String>();
        statusList.add(ANY);
        for (ExecutionStatus status : ExecutionStatus.values()) {
            statusList.add(status.toString());
        }
        statusSelect.setItems(statusList);
        statusSelect.setValue(ANY);
        statusSelect.addValueChangeListener(new ValueChangeListener<String>() {
            @Override
            public void valueChange(ValueChangeEvent<String> event) {
                refreshUI(getBackgroundData(), true);
            }
        });
        header.addComponent(statusSelect);
        header.setComponentAlignment(statusSelect, Alignment.BOTTOM_RIGHT);

        HorizontalLayout limitLayout = new HorizontalLayout();
        limitLayout.setSpacing(true);
        Label limitLabel = new Label("Limit:");
        limitLayout.addComponent(limitLabel);
        limitLayout.setComponentAlignment(limitLabel, Alignment.MIDDLE_CENTER);
        TextField limitField = new TextField(null, String.valueOf(DEFAULT_LIMIT));
        limitField.setWidth("5em");
        limitField.setValueChangeMode(ValueChangeMode.LAZY);
        limitField.setValueChangeTimeout(200);
        limitField.addValueChangeListener(new ValueChangeListener<String>() {
            public void valueChange(ValueChangeEvent<String> event) {
                try {
                    limit = Integer.parseInt(event.getValue());
                } catch (Exception e) {
                }
                refreshUI(getBackgroundData(), true);
            }
        });
        limitLayout.addComponent(limitField);
        limitLayout.setComponentAlignment(limitField, Alignment.BOTTOM_RIGHT);
        header.addComponent(limitLayout);
        header.setComponentAlignment(limitLayout, Alignment.BOTTOM_RIGHT);
        header.setExpandRatio(limitLayout, 1.0f);

        filterField = new TextField();
        filterField.setPlaceholder("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filterField.setIcon(VaadinIcons.SEARCH);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(200);
        filterField.addValueChangeListener(new ValueChangeListener<String>() {
            public void valueChange(ValueChangeEvent<String> event) {
                refreshGrid();
            }
        });
        header.addComponent(filterField);
        header.setComponentAlignment(filterField, Alignment.BOTTOM_RIGHT);

        header.setSpacing(true);
        header.setMargin(true);
        header.setWidth("100%");
        mainTab.addComponent(header);

        grid = new Grid<Execution>();
        grid.setSizeFull();
        grid.addItemClickListener(new ItemClickListener<Execution>() {
            @Override
            public void itemClick(ItemClick<Execution> event) {
                if (event.getMouseEventDetails().isDoubleClick()) {
                    viewLog(event.getItem());
                }
            }
        });
        grid.addColumn(Execution::getAgentName).setCaption("Agent").setWidth(250);
        grid.addColumn(Execution::getDeploymentName).setCaption("Deployment").setWidth(250);
        grid.addColumn(Execution::getHostName).setCaption("Host").setWidth(145).setHidden(true);
        grid.addColumn(Execution::getStatus).setCaption("Status").setWidth(90);
        grid.addColumn(item -> CommonUiUtils.formatDateTime(item.getStartTime())).setId("startTime")
                .setSortProperty("startTime").setCaption("Start").setWidth(170);
        grid.addColumn(item -> CommonUiUtils.formatDateTime(item.getEndTime())).setSortProperty("endTime")
                .setCaption("End").setWidth(170);
        grid.addColumn(Execution::getCreateBy).setCaption("Caller").setWidth(100);
        grid.addColumn(Execution::getParameters).setCaption("Parameters").setWidth(5000);
        for (Column<Execution, ?> column : grid.getColumns()) {
            column.setSortable(true).setHidable(true);
        }
        grid.sort("startTime", SortDirection.DESCENDING);
        grid.addSelectionListener((event) -> viewButton.setEnabled(event.getFirstSelectedItem().isPresent()));
        mainTab.addComponent(grid);
        mainTab.setExpandRatio(grid, 1.0f);

        tabs = new TabbedPanel();
        tabs.setMainTab("Executions", Icons.EXECUTION, mainTab);

        HorizontalSplitPanel split = new HorizontalSplitPanel();
        split.setSizeFull();
        split.setSplitPosition(UIConstants.DEFAULT_LEFT_SPLIT, Unit.PIXELS, false);

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
        split.setFirstComponent(manageNavigator);

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabs);
        split.setSecondComponent(container);

        addComponent(split);
        setSizeFull();
        context.getBackgroundRefresherService().register(this);
    }

    @Override
    public void enter(ViewChangeEvent event) {
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
        CommonUiUtils.notify(ex);        
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
            Execution currentGridSelection = ((SingleSelectionModel<Execution>) grid.getSelectionModel())
                    .getSelectedItem().orElse(null);
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
            tabs.addCloseableTab(execution.getId(), "Log " + execution.getFlowName(), Icons.LOG,
                    logPanel);
            logPanel.onBackgroundUIRefresh(logPanel.onBackgroundDataRefresh());
        }
    }

}
