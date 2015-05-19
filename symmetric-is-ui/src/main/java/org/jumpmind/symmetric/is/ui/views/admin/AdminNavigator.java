package org.jumpmind.symmetric.is.ui.views.admin;

import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class AdminNavigator extends VerticalLayout {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    TreeTable table;

    TabbedPanel tabbedPanel;

    public AdminNavigator(ApplicationContext context, TabbedPanel tabbedPanel) {

        this.context = context;
        this.tabbedPanel = tabbedPanel;
        setCaption("Navigator");
        setSizeFull();
        addStyleName("noborder");
        addStyleName(ValoTheme.MENU_ROOT);

        table = new TreeTable();
        table.addStyleName(ValoTheme.TREETABLE_NO_HORIZONTAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_NO_STRIPES);
        table.addStyleName(ValoTheme.TREETABLE_NO_VERTICAL_LINES);
        table.addStyleName(ValoTheme.TREETABLE_BORDERLESS);
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setContainerDataSource(new BeanItemContainer<NamedPanel>(NamedPanel.class));
        table.setVisibleColumns(new Object[] { "name" });
        table.setColumnExpandRatio("name", 1);
        table.addValueChangeListener(new TableValueChangeListener());
        
        addPanel(new UserPanel(context, tabbedPanel, "Users", Icons.USER));
        addPanel(new GroupPanel(context, tabbedPanel, "Groups", Icons.GROUP));
        addPanel(new ApiPanel(context, tabbedPanel, "REST", Icons.REST));

        table.addStyleName("noselect");
        addComponent(table);
        setExpandRatio(table, 1);
    }

    class TableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            NamedPanel panel = (NamedPanel) event.getProperty().getValue();
            if (panel != null) {
                Tab currentTab = tabbedPanel.getTab(0);
                if (currentTab != null) {
                    tabbedPanel.removeTab(currentTab);
                }
                tabbedPanel.addTab(panel, panel.getName(), panel.getIcon(), 0);
                tabbedPanel.setSelectedTab(0);
            }
        }
    }
    
    protected void addPanel(NamedPanel panel) {
        table.addItem(panel);
        table.setItemIcon(panel, panel.getIcon());
        table.setChildrenAllowed(panel, false);
        table.setCollapsed(panel, true);
    }
}
