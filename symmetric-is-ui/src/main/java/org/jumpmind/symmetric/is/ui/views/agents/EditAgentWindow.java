package org.jumpmind.symmetric.is.ui.views.agents;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentStartMode;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.support.IItemSavedListener;
import org.jumpmind.symmetric.is.ui.support.ResizableWindow;
import org.jumpmind.symmetric.is.ui.support.UiComponent;
import org.jumpmind.symmetric.is.ui.support.UiConstants;
import org.jumpmind.util.AppUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.server.FontAwesome;
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

    IItemSavedListener itemSavedListener;

    Agent agent;

    TextField nameField;

    TextField hostField;
    
    ComboBox agentStartModeCombo;

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

    public void show(Agent agent, IItemSavedListener itemSavedListener) {
        this.agent = agent;
        this.itemSavedListener = itemSavedListener;
        nameField.setValue(agent.getData().getName());
        hostField.setValue(agent.getData().getHost());
        agentStartModeCombo.setValue(agent.getAgentStartMode());
        resize(.6, true);
        nameField.focus();
    }

    public Agent getAgent() {
        return agent;
    }

    protected void save() {
        agent.getData().setName(nameField.getValue());
        agent.getData().setHost(hostField.getValue());
        agent.getData().setStartMode(((AgentStartMode)agentStartModeCombo.getValue()).name());
        configurationService.save(agent);

        // do some validation
        itemSavedListener.itemSaved(agent);

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
    
    public static void main(String[] args) {
        String value = "عععععععععععععع";
        System.out.println("size of " + value + " is " + value.length() + ".  The number of bytes is " + value.getBytes().length);
        value = substring(value, 10);
        System.out.println("size of " + value + " is " + value.length() + ".  The number of bytes is " + value.getBytes().length);
    }
    
    public static String substring(String value, int maxBytes) {
        StringBuilder ret = new StringBuilder();
        for(int i = 0;i < value.length(); i++) {
            if((maxBytes -= value.substring(i, i+1).getBytes().length) < 0) break;
            ret.append(value.charAt(i));
        }
        return ret.toString();
     }

}
