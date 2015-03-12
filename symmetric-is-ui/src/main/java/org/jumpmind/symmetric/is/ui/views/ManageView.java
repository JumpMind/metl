package org.jumpmind.symmetric.is.ui.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.FolderType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.IBackgroundRefreshable;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
import org.jumpmind.symmetric.is.ui.init.BackgroundRefresherService;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.TabbedApplicationPanel;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.MANAGE, name = "Manage", id = "manage", icon = FontAwesome.GEARS, menuOrder = 20)
public class ManageView extends HorizontalLayout implements View, IUiPanel, IBackgroundRefreshable {
    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IExecutionService executionService;

    @Autowired
    IComponentFactory componentFactory;

    @Autowired
    IResourceFactory resourceFactory;
    
    @Autowired
    BackgroundRefresherService backgroundRefresherService;

    ManageNavigator manageNavigator;

    DesignPropertySheet designPropertySheet;

    TabbedApplicationPanel tabs;

	BeanItemContainer<Execution> executionContainer = new BeanItemContainer<Execution>(Execution.class);
	
	Table table;
	
	Button viewButton;
	
    @SuppressWarnings("serial")
	@PostConstruct
    protected void init() {
        viewButton = new Button("View Log");
        viewButton.setEnabled(false);
        
        VerticalLayout mainTab = new VerticalLayout();
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		header.addComponent(viewButton);
		HorizontalLayout limitLayout = new HorizontalLayout();
		limitLayout.setSpacing(true);
		Label limitLabel = new Label("Limit:");
		limitLayout.addComponent(limitLabel);
		limitLayout.setComponentAlignment(limitLabel, Alignment.MIDDLE_CENTER);
		TextField limitField = new TextField(null, "100");
		limitField.setWidth("5em");
		limitLayout.addComponent(limitField);
		header.addComponent(limitLayout);
		header.setComponentAlignment(limitLayout, Alignment.MIDDLE_RIGHT);
		header.setExpandRatio(limitLayout, 1.0f);
		
        final TextField filterField = new TextField();
        filterField.setInputPrompt("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filterField.setIcon(FontAwesome.SEARCH);
        filterField.setImmediate(true);
        filterField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        filterField.setTextChangeTimeout(200);
        filterField.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                String filterCriteria = (String) event.getText();
                System.out.println("filter: " + filterCriteria);
            }
        });
        header.addComponent(filterField);

		header.setSpacing(true);
		header.setMargin(true);
		header.setWidth("100%");		
		mainTab.addComponent(header);
		
		table = new Table();
		table.setContainerDataSource(executionContainer);
		table.setSelectable(true);
		table.setMultiSelect(false);
		table.setSizeFull();
		table.setVisibleColumns(new Object[] { "agentName", "hostName", "flowName", "status", "startTime", "endTime" });
		table.setColumnHeaders(new String[] { "Agent", "Host", "Flow", "Status", "Start", "End"});
		table.addValueChangeListener(new ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				viewButton.setEnabled(table.getValue() != null);
			}
		});
		mainTab.addComponent(table);
		mainTab.setExpandRatio(table, 1.0f);
		
        tabs = new TabbedApplicationPanel();        
        tabs.addTab(mainTab, "Executions", Icons.EXECUTION);

        HorizontalSplitPanel split = new HorizontalSplitPanel();
        split.setSizeFull();
        split.setSplitPosition(300, Unit.PIXELS, false);
        
        manageNavigator = new ManageNavigator(FolderType.RUNTIME, configurationService);
        manageNavigator.addValueChangeListener(new ValueChangeListener() {
			public void valueChange(ValueChangeEvent event) {
				refreshUI(getBackgroundData());
			}
        });
        split.setFirstComponent(manageNavigator);

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabs);
        split.setSecondComponent(container);

        addComponent(split);
        setSizeFull();
        backgroundRefresherService.register(this);
    }

    @Override
    public void enter(ViewChangeEvent event) {
    	manageNavigator.refresh();
    }

    @Override
    public boolean closing() {
        backgroundRefresherService.unregister(this);
        return true;
    }

    @Override
    public void showing() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object onBackgroundDataRefresh() {
        return getBackgroundData();
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
    	refreshUI(backgroundData);
    }

    public Object getBackgroundData() {
    	Object currentSelection = manageNavigator.getCurrentSelection();
    	if (currentSelection != null) {
        	Map<String, Object> params = new HashMap<String, Object>();
    		if (currentSelection instanceof Agent) {
    			params.put("agentId", ((Agent) currentSelection).getId());
    		} else if (currentSelection instanceof FlowVersion) {
    			params.put("flowVersionId", ((FlowVersion) currentSelection).getId());    			
    		}

    		if (params.size() > 0) {
    			return executionService.findExecutions(params);		
    		}
    	}
    	return null;
    }

    @SuppressWarnings("unchecked")
	protected void refreshUI(Object data) {
    	Object currentSelection = table.getValue();
    	executionContainer.removeAllItems();
    	table.setValue(null);
    	if (data != null) {
    		executionContainer.addAll((List<Execution>) data);
    		table.setValue(currentSelection);
    	}
    	viewButton.setEnabled(table.getValue() != null);
    }

}
