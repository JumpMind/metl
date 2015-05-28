package org.jumpmind.symmetric.is.ui.views;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.ui.common.AppConstants;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.IBackgroundRefreshable;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
import org.jumpmind.symmetric.is.ui.views.admin.AboutPanel;
import org.jumpmind.symmetric.is.ui.views.admin.ApiPanel;
import org.jumpmind.symmetric.is.ui.views.admin.EmailServerPanel;
import org.jumpmind.symmetric.is.ui.views.admin.GroupPanel;
import org.jumpmind.symmetric.is.ui.views.admin.LoggingPanel;
import org.jumpmind.symmetric.is.ui.views.admin.UserPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Item;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
@TopBarLink(category = Category.ADMIN, name = "Admin", id = "admin", icon = FontAwesome.GEARS, menuOrder = 10)
public class AdminView extends HorizontalLayout implements View, IUiPanel, IBackgroundRefreshable, ItemClickListener {

    private static final long serialVersionUID = 1L;

    @Autowired
    ApplicationContext context;

    TabbedPanel tabbedPanel;
    
    TreeTable table;

    @PostConstruct
    protected void init() {
        setSizeFull();

        tabbedPanel = new TabbedPanel();

        HorizontalSplitPanel leftSplit = new HorizontalSplitPanel();
        leftSplit.setSizeFull();
        leftSplit.setSplitPosition(AppConstants.DEFAULT_LEFT_SPLIT, Unit.PIXELS);

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.addComponent(tabbedPanel);
        leftSplit.setSecondComponent(container);

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
        table.addItemClickListener(this);
        table.addStyleName("noselect");
        table.addContainerProperty("id", String.class, null);
        table.setVisibleColumns(new Object[] { "id" });
        table.setColumnExpandRatio("id", 1);
        addItem("Users", Icons.USER);
        addItem("Groups", Icons.GROUP);
        addItem("REST", Icons.REST);
        addItem("Email", Icons.EMAIL);
        addItem("Logging", Icons.LOGGING);
        addItem("About", FontAwesome.QUESTION);
        
        VerticalLayout navigator = new VerticalLayout(table);
        navigator.setSizeFull();
        navigator.setExpandRatio(table, 1);
        leftSplit.setFirstComponent(navigator);
        addComponent(leftSplit);
        
        context.getBackgroundRefresherService().register(this);
    }

    @SuppressWarnings("unchecked")
    protected void addItem(String id, Resource icon) {
        Item item = table.addItem(id);
        item.getItemProperty("id").setValue(id);
        table.setItemIcon(id, icon);
        table.setChildrenAllowed(id, false);
        table.setCollapsed(id, true);
    }

    public void itemClick(ItemClickEvent event) {
        if (event.getButton() == MouseButton.LEFT) {
            if (event.isDoubleClick()) {
                Object value = event.getItemId();
                if (value != null) {
                    String id = value.toString();
                    Component panel = null;
                    if (id.equals("Users")) {
                        panel = new UserPanel(context, tabbedPanel);
                    } else if (id.equals("Groups")) {
                        panel = new GroupPanel(context, tabbedPanel);
                    } else if (id.equals("REST")) {
                        panel = new ApiPanel(context, tabbedPanel);
                    } else if (id.equals("Email")) {
                        panel = new EmailServerPanel(context, tabbedPanel);
                    } else if (id.equals("Logging")) {
                        panel = new LoggingPanel(context, tabbedPanel);
                    } else if (id.equals("About")) {
                        panel = new AboutPanel(context, tabbedPanel);
                    }
                    tabbedPanel.addCloseableTab(id, id, table.getItemIcon(id), panel);
                }
            }
        }
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }

    @Override
    public <T> T onBackgroundDataRefresh() {
        return null;
    }

    @Override
    public <T> void onBackgroundUIRefresh(T backgroundData) {
    }

    @Override
    public boolean closing() {
        return false;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {        
    }
}
