package org.jumpmind.symmetric.is.ui.views.manage;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentStartMode;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.ui.common.IItemUpdatedListener;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.jumpmind.symmetric.ui.common.UiConstants;
import org.jumpmind.util.AppUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
public class EditAgentWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    IItemUpdatedListener itemSavedListener;

    Agent agent;

    TextField nameField;

    TextField hostField;
    
    AbstractSelect agentStartModeCombo;

    public EditAgentWindow() {
        setCaption("Edit Agent");

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        Component comp = buildSettingsLayout();
        content.addComponent(comp);
        content.setExpandRatio(comp, 1);
        
        Button cancelButton = new Button("Cancel", new CloseButtonListener());
        Button saveButton = new Button("Save", new SaveClickListener());
        saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        content.addComponent(buildButtonFooter(new Button[0], new Button[] { cancelButton,
                saveButton }));

        nameField.focus();

    }
    
    protected VerticalLayout buildSettingsLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSizeFull();
        layout.addStyleName("v-scrollable");

        FormLayout formLayout = new FormLayout();
        layout.addComponent(formLayout);        

        nameField = new TextField("Name");
        nameField.setImmediate(true);
        nameField.setWidth(UiConstants.TEXTFIELD_WIDTH, Unit.PIXELS);
        nameField.setNullRepresentation("");
        formLayout.addComponent(nameField);

        CssLayout hostComponentGroup = new CssLayout();
        hostComponentGroup.setCaption("Host");
        hostComponentGroup.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        formLayout.addComponent(hostComponentGroup);

        hostField = new TextField();
        hostField.setImmediate(true);
        hostField.setWidth(UiConstants.TEXTFIELD_WIDTH, Unit.PIXELS);
        hostField.setNullRepresentation("");
        hostField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        hostField.setIcon(FontAwesome.LAPTOP);

        hostComponentGroup.addComponent(hostField);

        Button button = new Button("Get", new GetHostClickListener());
        hostComponentGroup.addComponent(button);
        
        agentStartModeCombo = new ComboBox("Agent Start Mode");
        agentStartModeCombo.setNullSelectionAllowed(false);
        AgentStartMode[] modes = AgentStartMode.values();
        for (AgentStartMode agentStartMode : modes) {
            agentStartModeCombo.addItem(agentStartMode);
        }
        formLayout.addComponent(agentStartModeCombo);

        return layout;
    }

    public void show(Agent agent, IItemUpdatedListener itemSavedListener) {
        this.agent = agent;
        this.itemSavedListener = itemSavedListener;
        nameField.setValue(agent.getName());
        hostField.setValue(agent.getHost());
        agentStartModeCombo.setValue(agent.getAgentStartMode());
        showAtSize(.6);
        nameField.focus();
    }

    public Agent getAgent() {
        return agent;
    }

    protected void save() {
        agent.setName(nameField.getValue());
        agent.setHost(hostField.getValue());
        agent.setStartMode(((AgentStartMode)agentStartModeCombo.getValue()).name());
        configurationService.save(agent);

        // do some validation
        itemSavedListener.itemUpdated(agent);

        close();
    }

    class SaveClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            save();
        }

    }

    class GetHostClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            hostField.setValue(AppUtils.getHostName());
        }

    }

}
