package org.jumpmind.symmetric.is.ui.views.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentStatus;
import org.jumpmind.symmetric.is.core.model.Notification;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextArea;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class NotificationEditPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Notification notification;
    
    NativeSelect levelField;
    
    ComboBox linkField;

    NativeSelect eventField;
    
    ImmediateUpdateTextField nameField;
    
    ImmediateUpdateTextField subjectField;
    
    ImmediateUpdateTextArea messageField;
    
    Map<String, String> sampleSubjectByEvent;
    
    Map<String, String> sampleMessageByEvent;
    
    boolean autoSave;
    
    boolean isInit;
    
    boolean isChanged;
    
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
        levelField.addValueChangeListener(new LevelFieldListener());
        form.addComponent(levelField);

        linkField = new ComboBox("Linked To");
        linkField.setNullSelectionAllowed(false);
        linkField.setImmediate(true);
        linkField.setWidth(15f, Unit.EM);
        linkField.addValueChangeListener(new LinkFieldListener());
        form.addComponent(linkField);

        eventField = new NativeSelect("Event");
        eventField.setNullSelectionAllowed(false);
        eventField.setImmediate(true);
        eventField.setWidth(15f, Unit.EM);
        eventField.addValueChangeListener(new EventFieldListener());
        form.addComponent(eventField);

        nameField = new ImmediateUpdateTextField("Name") {
            protected void save(String value) {
                notification.setName(value);
                saveNotification();
            }            
        };
        nameField.setValue(StringUtils.trimToEmpty(notification.getName()));
        nameField.setWidth(20f, Unit.EM);
        nameField.setDescription("Display name for the notification");
        form.addComponent(nameField);

        ImmediateUpdateTextArea recipientsField = new ImmediateUpdateTextArea("Recipients") {
            protected void save(String value) {
                notification.setRecipients(value);
                saveNotification();
            }                        
        };
        recipientsField.setValue(StringUtils.trimToEmpty(notification.getRecipients()));
        recipientsField.setColumns(20);
        recipientsField.setRows(10);
        recipientsField.setInputPrompt("address1@example.com\r\naddress2@example.com");
        recipientsField.setDescription("Email addresses of recipients, separated by commas.");
        form.addComponent(recipientsField);

        subjectField = new ImmediateUpdateTextField("Subject") {
            protected void save(String value) {
                notification.setSubject(value);
                saveNotification();
            }            
        };
        subjectField.setValue(StringUtils.trimToEmpty(notification.getSubject()));
        subjectField.setWidth(40f, Unit.EM);
        subjectField.setDescription("The subject of the email can contain...");
        form.addComponent(subjectField);

        messageField = new ImmediateUpdateTextArea("Message") {
            protected void save(String value) {
                notification.setMessage(value);
                saveNotification();
            }                        
        };
        messageField.setValue(StringUtils.trimToEmpty(notification.getMessage()));
        messageField.setColumns(40);
        messageField.setRows(10);
        messageField.setDescription("The body of the email can contain...");
        form.addComponent(messageField);
        
        CheckBox enableField = new CheckBox("Enabled", notification.isEnabled());
        enableField.setImmediate(true);
        enableField.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                notification.setEnabled((Boolean) event.getProperty().getValue());
                saveNotification();
            }            
        });
        form.addComponent(enableField);
        
        if (notification.getLevel() == null) {
            isInit = true;
            levelField.setValue(Notification.Level.GLOBAL.toString());
            notification.setLevel(Notification.Level.GLOBAL.toString());
            notification.setNotifyType(Notification.NotifyType.MAIL.toString());
            updateLinks();
            updateEventTypes();
            updateName();
        } else {
            levelField.setValue(notification.getLevel());
            updateLinks();
            updateEventTypes();
            linkField.setValue(notification.getLinkId());
            eventField.setValue(notification.getEventType());
            isInit = true;
        }

        addComponent(form);
        setMargin(true);
        autoSave = true;
    }

    @Override
    public boolean closing() {
        if (isChanged) {
            String level = notification.getLevel();
            if (level.equals(Notification.Level.GLOBAL.toString())) {
                for (Agent agent : context.getConfigurationService().findAgents()) {
                    refreshAgent(agent);
                }
            } else if (level.equals(Notification.Level.AGENT.toString()) && notification.getLinkId() != null) {
                refreshAgent(context.getConfigurationService().findAgent(notification.getLinkId()));
            } else if (level.equals(Notification.Level.DEPLOYMENT.toString()) && notification.getLinkId() != null) {
                AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(notification.getLinkId());
                if (deployment != null) {
                    refreshAgent(context.getConfigurationService().findAgent(deployment.getAgentId()));
                }
            }
        }
        return true;
    }
    
    private void refreshAgent(Agent agent) {
        if (agent != null && !agent.isDeleted() && agent.getStatus().equals(AgentStatus.RUNNING.name())) {
            agent.setStatus(AgentStatus.REQUEST_REFRESH.name());
            context.getConfigurationService().save(agent);
        }        
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    private void saveNotification() {
        if (autoSave) {
            context.getConfigurationService().save(notification);
        }
        isChanged = true;
    }

    private void updateLinks() {
        String level = (String) levelField.getValue();
        linkField.removeAllItems();
        if (level.equals(Notification.Level.GLOBAL.toString())) {
            linkField.setEnabled(false);
        } else if (level.equals(Notification.Level.AGENT.toString())) {
            linkField.setEnabled(true);
            for (Agent agent : context.getConfigurationService().findAgents()) {
                if (agent.isDeleted()) {
                    linkField.addItem(agent.getId());
                    linkField.setItemCaption(agent.getId(), agent.getName());
                }
            }
        } else if (level.equals(Notification.Level.DEPLOYMENT.toString())) {
            linkField.setEnabled(true);
            List<Agent> agents = context.getConfigurationService().findAgents();
            for (Agent agent : agents) {
                if (!agent.isDeleted()) {
                    context.getConfigurationService().refresh(agent);
                    for (AgentDeployment deployment : agent.getAgentDeployments()) {
                        linkField.addItem(deployment.getId());
                        linkField.setItemCaption(deployment.getId(), agent.getName() + "/" + deployment.getName());                        
                    }
                }
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
        if (isInit) {
            eventField.setValue(eventTypes[0].toString());
            notification.setEventType((String) eventField.getValue());
        }
    }

    private void updateName() {
        if (isInit) {
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
            notification.setName(nameField.getValue());
            notification.setSubject(subjectField.getValue());
            notification.setMessage(messageField.getValue());
        }
    }

    class LevelFieldListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            boolean oldAutoSave = autoSave;
            autoSave = false;
            notification.setLevel((String) levelField.getValue());
            updateEventTypes();
            updateLinks();
            autoSave = oldAutoSave;
            saveNotification();
        }
    }

    class LinkFieldListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            boolean oldAutoSave = autoSave;
            autoSave = false;
            notification.setLinkId((String) linkField.getValue());
            updateName();
            autoSave = oldAutoSave;
            saveNotification();
        }
    }

    class EventFieldListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            boolean oldAutoSave = autoSave;
            autoSave = false;
            notification.setEventType((String) eventField.getValue());
            updateName();
            autoSave = oldAutoSave;
            saveNotification();
        }
    }

}
