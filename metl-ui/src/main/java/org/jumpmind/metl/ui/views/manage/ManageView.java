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
import java.util.Collection;
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
import org.jumpmind.metl.ui.common.MultiPropertyFilter;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.Table;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.Manage, name = "Manage", id = "manage", icon = FontAwesome.GEARS, menuOrder = 25)
public class ManageView extends HorizontalLayout implements View, IUiPanel, IBackgroundRefreshable<Object> {
    
    final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ANY = "<Any>";

    private static final long serialVersionUID = 1L;

    static final int DEFAULT_LIMIT = 100;

    @Autowired
    ApplicationContext context;

    ManageNavigator manageNavigator;

    TabbedPanel tabs;

    BeanItemContainer<Execution> executionContainer = new BeanItemContainer<Execution>(
            Execution.class);

    Table table;

    Button viewButton;

    AbstractSelect statusSelect;

    int limit = DEFAULT_LIMIT;

    @SuppressWarnings("serial")
    @PostConstruct
    protected void init() {
        viewButton = new Button("View Log");
        viewButton.setEnabled(false);
        viewButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                viewLog(table.getValue());
            }
        });

        VerticalLayout mainTab = new VerticalLayout();
        mainTab.setSizeFull();
        HorizontalLayout header = new HorizontalLayout();
        header.addComponent(viewButton);
        header.setComponentAlignment(viewButton, Alignment.BOTTOM_RIGHT);

        statusSelect = new ComboBox("Status");
        statusSelect.setNewItemsAllowed(false);
        statusSelect.setNullSelectionAllowed(false);
        statusSelect.addItem(ANY);
        statusSelect.setValue(ANY);
        for (ExecutionStatus status : ExecutionStatus.values()) {
            statusSelect.addItem(status.toString());
        }
        ;
        statusSelect.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
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
        limitField.setImmediate(true);
        limitField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        limitField.setTextChangeTimeout(200);
        limitField.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                try {
                    limit = Integer.parseInt(event.getText());
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

        TextField filterField = new TextField();
        filterField.setInputPrompt("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filterField.setIcon(FontAwesome.SEARCH);
        filterField.setImmediate(true);
        filterField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        filterField.setTextChangeTimeout(200);
        filterField.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                executionContainer.removeAllContainerFilters();
                if (!StringUtils.isBlank(event.getText())) {
                    executionContainer.addContainerFilter(
                            new MultiPropertyFilter(event.getText(), new String[] { "agentName",
                                    "hostName", "flowName", "status", "startTime", "endTime" }));
                }
            }
        });
        header.addComponent(filterField);
        header.setComponentAlignment(filterField, Alignment.BOTTOM_RIGHT);

        header.setSpacing(true);
        header.setMargin(true);
        header.setWidth("100%");
        mainTab.addComponent(header);

        table = new Table();
        table.setContainerDataSource(executionContainer);
        table.setSelectable(true);
        table.setMultiSelect(false);
        table.setSizeFull();
        table.setColumnCollapsingAllowed(true);
        table.addItemClickListener(new ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    viewLog(event.getItemId());
                }
            }
        });
        table.setVisibleColumns(new Object[] { "agentName", "deploymentName", "hostName", "status",
                "startTime", "endTime", "createBy", "parameters" });
        table.setColumnHeaders(new String[] { "Agent", "Deployment", "Host", "Status", "Start",
                "End", "Caller", "Parameters" });
        table.setColumnWidth("agentName", 250);
        table.setColumnWidth("deploymentName", 250);
        table.setColumnWidth("hostName", 145);
        table.setColumnWidth("status", 90);
        table.setColumnWidth("startTime", 170);
        table.setColumnWidth("endTime", 170);
        table.setColumnWidth("createBy", 100);
        table.setColumnWidth("parameters", 5000);
        table.setColumnCollapsed("hostName", true);
        table.setSortContainerPropertyId("startTime");
        table.setSortAscending(false);
        table.addValueChangeListener((event) -> viewButton.setEnabled(table.getValue() != null));
        mainTab.addComponent(table);
        mainTab.setExpandRatio(table, 1.0f);

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
            Object currentTableSelection = table.getValue();
            table.setValue(null);
            table.removeAllItems();
            if (data != null) {
                executionContainer.addAll((List<Execution>) data);
                table.sort();
                table.setValue(currentTableSelection);
                table.refreshRowCache();
            }
            viewButton.setEnabled(table.getValue() != null);
            if (tabToFront) {
                tabs.mainTabToTop();
            }
        }
    }

    protected boolean needsUpdated(List<Execution> data) {
        boolean needsUpdated = false;
        List<Execution> all = data != null ? new ArrayList<Execution>(data)
                : new ArrayList<Execution>(0);
        @SuppressWarnings("unchecked")
        Collection<Execution> tableValues = (Collection<Execution>) table.getItemIds();

        if (all.size() != tableValues.size()) {
            log.debug("new execution count = " + all.size() + ", old execution count = " + tableValues.size());
            needsUpdated = true;
        }
        
        if (!needsUpdated) {
            for (Execution execution : tableValues) {
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
            all.removeAll(tableValues);
            log.debug("different execution count = " + all.size());
            needsUpdated = all.size() > 0;
        }

        return needsUpdated;
    }

    protected void viewLog(Object item) {
        Execution execution = (Execution) item;
        ExecutionRunPanel logPanel = new ExecutionRunPanel(execution.getId(), context, tabs, null);
        tabs.addCloseableTab(execution.getId(), "Log " + execution.getFlowName(), Icons.LOG,
                logPanel);
        logPanel.onBackgroundUIRefresh(logPanel.onBackgroundDataRefresh());
    }

}
