package org.jumpmind.symmetric.is.ui.views.deprecated;

import java.util.HashSet;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.FlowVersionSummary;
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
public class SelectComponentFlowVersionWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    IItemUpdatedListener itemUpdatedListener;

    BeanItemContainer<FlowVersionSummary> container;

    MultiSelectTable table;

    public SelectComponentFlowVersionWindow() {
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
        container = new BeanItemContainer<FlowVersionSummary>(
                FlowVersionSummary.class);
        table.setContainerDataSource(container);
        table.setSizeFull();

        table.setColumnHeader("name", "Flow Name");
        table.setColumnHeader("versionName", "Version Name");
        table.setColumnHeader("folderName", "Folder Name");

        table.setVisibleColumns("name", "versionName", "folderName");
        table.setColumnExpandRatio("folderName", 1);

        layout.addComponent(table);
        layout.setExpandRatio(table, 1);

        return layout;
    }

    public void show(Agent agent, IItemUpdatedListener itemUpdatedListener) {
        this.itemUpdatedListener = itemUpdatedListener;
        setCaption("Select Flows to Deploy");
        this.container.removeAllItems();
        this.container.addAll(configurationService.findUndeployedFlowVersionSummary(agent
                .getId()));
        this.table.setValue(new HashSet<FlowVersionSummary>());
        showAtSize(.5);
    }

    protected void done() {
        Set<FlowVersionSummary> selectedFlows = table
                .getSelected();
        itemUpdatedListener.itemUpdated(selectedFlows);
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
