package org.jumpmind.symmetric.is.ui.views.design;

import java.util.HashSet;
import java.util.Set;

import org.jumpmind.symmetric.is.core.config.AgentSummary;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersionSummary;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.ui.common.IItemUpdatedListener;
import org.jumpmind.symmetric.ui.common.MultiSelectTable;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
public class SelectAgentsWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    IItemUpdatedListener itemUpdatedListener;

    BeanItemContainer<AgentSummary> container;

    MultiSelectTable table;

    public SelectAgentsWindow() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        Component comp = buildMainLayout();
        content.addComponent(comp);
        content.setExpandRatio(comp, 1);

        Button closeButton = new Button("Select", new CloseClickListener());
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        content.addComponent(buildButtonFooter(new Button[0], new Button[] { closeButton }));

    }

    protected VerticalLayout buildMainLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setSizeFull();

        TextField searchField = new TextField();
        searchField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        searchField.setIcon(FontAwesome.SEARCH);
        searchField.setInputPrompt("Search Flows");
        layout.addComponent(searchField);

        table = new MultiSelectTable();
        container = new BeanItemContainer<AgentSummary>(AgentSummary.class);
        table.setContainerDataSource(container);
        table.setSizeFull();

        table.setColumnHeader("name", "Agent Name");
        table.setColumnHeader("host", "Host Name");
        table.setColumnHeader("folderName", "Folder Name");

        table.setVisibleColumns("name", "host", "folderName");
        table.setColumnExpandRatio("folderName", 1);

        layout.addComponent(table);
        layout.setExpandRatio(table, 1);

        return layout;
    }

    public void show(String componentFlowVersionId, IItemUpdatedListener itemUpdatedListener) {
        this.itemUpdatedListener = itemUpdatedListener;
        setCaption("Select Agents To Deploy To");
        this.container.removeAllItems();
        this.container.addAll(configurationService.findUndeployedAgentsFor(componentFlowVersionId));
        this.table.setValue(new HashSet<ComponentFlowVersionSummary>());
        showAtSize(.5);
    }

    protected void done() {
        Set<AgentSummary> agents = table.getSelected();
        itemUpdatedListener.itemUpdated(agents);
        close();
    }

    class CloseClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            done();
        }

    }

}
