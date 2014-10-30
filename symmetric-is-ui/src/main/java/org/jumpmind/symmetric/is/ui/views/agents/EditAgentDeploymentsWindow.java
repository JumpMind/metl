package org.jumpmind.symmetric.is.ui.views.agents;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.support.IItemSavedListener;
import org.jumpmind.symmetric.is.ui.support.ResizableWindow;
import org.jumpmind.symmetric.is.ui.support.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
public class EditAgentDeploymentsWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    IItemSavedListener itemSavedListener;

    Agent agent;

    public EditAgentDeploymentsWindow() {

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        Component comp = buildMainLayout();
        content.addComponent(comp);
        content.setExpandRatio(comp, 1);
        
        Button closeButton = new Button("Close", new CloseClickListener());
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        content.addComponent(buildButtonFooter(new Button[0], new Button[] { 
                closeButton }));

    }
    
    protected VerticalLayout buildMainLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSizeFull();
        layout.addStyleName("v-scrollable");

        return layout;
    }

    public void show(Agent agent, IItemSavedListener itemSavedListener) {        
        this.agent = agent;
        this.itemSavedListener = itemSavedListener;
        setCaption("Agent Deployments for '" + agent.toString() + "'");
        resize(.6, true);
    }

    public Agent getAgent() {
        return agent;
    }

    protected void done() {
        itemSavedListener.itemSaved(agent);
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
