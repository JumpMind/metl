package org.jumpmind.symmetric.is.ui.views.agents;

import org.jumpmind.symmetric.is.core.config.Agent;
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

    public EditAgentWindow() {

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        VerticalLayout topLayout = new VerticalLayout();
        topLayout.setMargin(true);
        topLayout.setSizeFull();
        topLayout.addStyleName("v-scrollable");
        content.addComponent(topLayout);
        content.setExpandRatio(topLayout, 1);

        FormLayout formLayout = new FormLayout();
        topLayout.addComponent(formLayout);

        nameField = new TextField("Name");
        nameField.setWidth(UiConstants.TEXTFIELD_WIDTH, Unit.PIXELS);
        nameField.setNullRepresentation("");
        formLayout.addComponent(nameField);

        CssLayout hostComponentGroup = new CssLayout();
        hostComponentGroup.setCaption("Host");
        hostComponentGroup.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
        formLayout.addComponent(hostComponentGroup);

        hostField = new TextField();
        hostField.setWidth(UiConstants.TEXTFIELD_WIDTH, Unit.PIXELS);
        hostField.setNullRepresentation("");
        hostField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        hostField.setIcon(FontAwesome.LAPTOP);

        hostComponentGroup.addComponent(hostField);

        Button button = new Button("Get", new GetHostClickListener());
        hostComponentGroup.addComponent(button);

        Button cancelButton = new Button("Cancel", new CloseButtonListener());
        Button saveButton = new Button("Save", new SaveClickListener());
        saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        content.addComponent(buildButtonFooter(new Button[0], new Button[] { cancelButton,
                saveButton }));

        nameField.focus();

    }

    public void show(Agent agent, IItemSavedListener itemSavedListener) {

        this.agent = agent;

        this.itemSavedListener = itemSavedListener;

        nameField.setValue(agent.getData().getName());

        hostField.setValue(agent.getData().getHost());

        setCaption("Edit Agent");

        resize(.6, true);

        nameField.focus();

    }

    public Agent getAgent() {
        return agent;
    }

    protected void save() {
        agent.getData().setName(nameField.getValue());
        agent.getData().setHost(hostField.getValue());
        
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

}
