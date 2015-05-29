package org.jumpmind.symmetric.is.ui.views.admin;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Notification;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class NotificationEditPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Notification notification;
    
    NativeSelect levelField;
    
    ComboBox linkField;

    NativeSelect eventField;
    
    TextField nameField;
    
    TextField subjectField;
    
    TextArea recipientsField;
    
    TextArea messageField;
    
    CheckBox enableField;
    
    Map<String, String> sampleSubjectByEvent;
    
    Map<String, String> sampleMessageByEvent;
    
    public NotificationEditPanel(final ApplicationContext context, final Notification notification) {
        this.context = context;
        this.notification = notification;

        sampleSubjectByEvent = new HashMap<String, String>();
        sampleSubjectByEvent.put(Notification.EventType.FLOW_START.toString(), "Flow $(flow) started");
        sampleSubjectByEvent.put(Notification.EventType.FLOW_END.toString(), "Flow $(flow) ended");
        sampleSubjectByEvent.put(Notification.EventType.FLOW_ERROR.toString(), "Flow $(flow) in ERROR");

        sampleMessageByEvent = new HashMap<String, String>();
        sampleMessageByEvent.put(Notification.EventType.FLOW_START.toString(), "Started flow $(flow) on agent $(agent) at $(time) on $(date)");
        sampleMessageByEvent.put(Notification.EventType.FLOW_END.toString(), "Ended flow $(flow) on agent $(agent) at $(time) on $(date)");
        sampleMessageByEvent.put(Notification.EventType.FLOW_ERROR.toString(), "Error in flow $(flow) on agent $(agent) at $(time) on $(date)");
        
        FormLayout form = new FormLayout();
        form.setSizeFull();
        form.setSpacing(true);

        levelField = new NativeSelect("Level");
        for (Notification.Level level : Notification.Level.values()) {
            levelField.addItem(level.toString());
        }
        levelField.setNullSelectionAllowed(false);
        levelField.setImmediate(true);
        levelField.setWidth(15f, Unit.EM);
        form.addComponent(levelField);

        linkField = new ComboBox("Linked To");
        linkField.setNullSelectionAllowed(false);
        linkField.setImmediate(true);
        linkField.setWidth(15f, Unit.EM);
        form.addComponent(linkField);

        eventField = new NativeSelect("Event");
        eventField.setNullSelectionAllowed(false);
        eventField.setImmediate(true);
        eventField.setWidth(15f, Unit.EM);
        form.addComponent(eventField);

        nameField = new TextField("Name", StringUtils.trimToEmpty(notification.getName()));
        nameField.setWidth(20f, Unit.EM);
        nameField.setDescription("Display name for the notification");
        nameField.setImmediate(true);
        form.addComponent(nameField);

        recipientsField = new TextArea("Recipients", StringUtils.trimToEmpty(notification.getRecipients()));
        recipientsField.setColumns(20);
        recipientsField.setRows(10);
        recipientsField.setInputPrompt("address1@example.com\r\naddress2@example.com");
        recipientsField.setDescription("Email addresses of recipients, separated by commas or whitespace");
        recipientsField.setImmediate(true);
        form.addComponent(recipientsField);
        
        subjectField = new TextField("Subject", StringUtils.trimToEmpty(notification.getSubject()));
        subjectField.setWidth(40f, Unit.EM);
        subjectField.setDescription("The subject of the email can contain...");
        subjectField.setImmediate(true);
        form.addComponent(subjectField);

        messageField = new TextArea("Message", StringUtils.trimToEmpty(notification.getMessage()));
        messageField.setColumns(40);
        messageField.setRows(10);
        messageField.setDescription("The body of the email can contain...");
        messageField.setImmediate(true);
        form.addComponent(messageField);
        
        enableField = new CheckBox("Enabled", notification.isEnabled());
        enableField.setImmediate(true);
        form.addComponent(enableField);
        
        if (notification.getLevel() == null) {
            levelField.setValue(Notification.Level.GLOBAL.toString());
            updateLinks();
            updateEventTypes();
            updateName();
            updateNotification();
        } else {
            levelField.setValue(notification.getLevel());
            updateLinks();
            updateEventTypes();
            linkField.setValue(notification.getLinkId());
            eventField.setValue(notification.getEventType());
        }

        levelField.addValueChangeListener(new LevelChangeListener());
        linkField.addValueChangeListener(new LinkChangeListener());
        eventField.addValueChangeListener(new EventChangeListener());

        ValueChangeListener listener = new FieldChangeListener();
        nameField.addValueChangeListener(listener);
        recipientsField.addValueChangeListener(listener);
        subjectField.addValueChangeListener(listener);
        messageField.addValueChangeListener(listener);
        enableField.addValueChangeListener(listener);

        addComponent(form);
        setMargin(true);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    private void saveNotification() {
        updateNotification();
        context.getConfigurationService().save(notification);        
    }
    
    private void updateNotification() {
        notification.setLevel((String) levelField.getValue());
        notification.setLinkId((String) linkField.getValue());
        notification.setEventType((String) eventField.getValue());
        notification.setName((String) nameField.getValue());
        notification.setRecipients((String) recipientsField.getValue());
        notification.setSubject((String) subjectField.getValue());
        notification.setMessage((String) messageField.getValue());
        notification.setNotifyType(Notification.NotifyType.MAIL.toString());
        notification.setEnabled(enableField.getValue());
    }

    private void updateLinks() {
        String level = (String) levelField.getValue();
        linkField.removeAllItems();
        if (level.equals(Notification.Level.GLOBAL.toString())) {
            linkField.setEnabled(false);
        } else if (level.equals(Notification.Level.AGENT.toString())) {
            linkField.setEnabled(true);
            for (Agent agent : context.getConfigurationService().findAgents()) {
                linkField.addItem(agent.getId());
                linkField.setItemCaption(agent.getId(), agent.getName());
            }
        } else if (level.equals(Notification.Level.DEPLOYMENT.toString())) {
            linkField.setEnabled(true);
            for (AgentDeployment deployment : context.getConfigurationService().findAgentDeployments()) {
                linkField.addItem(deployment.getId());
                linkField.setItemCaption(deployment.getId(), deployment.getName());
            }
        }
    }
    
    private void updateEventTypes() {
        String level = (String) levelField.getValue();
        Notification.EventType[] eventTypes = Notification.getEventTypesForLevel(level);
        eventField.removeAllItems();
        for (Notification.EventType eventType : eventTypes) {
            eventField.addItem(eventType.toString());
        }
        eventField.setValue(eventTypes[0].toString());
    }

    private void updateName() {
        String name = "";
        if (levelField.getValue().equals(Notification.Level.GLOBAL.toString())) {
            name = "GLOBAL " + eventField.getValue();
        } else {
            if (linkField.getValue() != null) {
                name = linkField.getItemCaption(linkField.getValue()) + " - ";
            }
            name += eventField.getValue();
        }
        nameField.setValue(name);
        subjectField.setValue(sampleSubjectByEvent.get(eventField.getValue()));
        messageField.setValue(sampleMessageByEvent.get(eventField.getValue()));
    }

    class LevelChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            updateLinks();
            updateEventTypes();
            saveNotification();
        }
    }

    class LinkChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            updateName();
            saveNotification();
        }
    }

    class EventChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            updateName();
            saveNotification();
        }
    }

    class FieldChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            saveNotification();
        }
    }

}
