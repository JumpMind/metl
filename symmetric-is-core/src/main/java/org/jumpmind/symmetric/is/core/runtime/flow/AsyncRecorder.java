package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.time.DateFormatUtils;
import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStatus;
import org.jumpmind.symmetric.is.core.model.MailServer;
import org.jumpmind.symmetric.is.core.model.Notification;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;
import org.jumpmind.util.AppUtils;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncRecorder implements Runnable {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    private static final String TIME_FORMAT = "HH:mm:ss";
    
    protected BlockingQueue<AbstractObject> inQueue;

    protected IExecutionService executionService;

    protected boolean running = false;
    
    protected boolean stopping = false;
    
    protected MailServer mailServer;
    
    protected Map<String, List<Notification>> notificationMap;
    
    protected Session session;

    public AsyncRecorder(IExecutionService executionService, MailServer mailServer, List<Notification> notifications) {
        this.inQueue = new LinkedBlockingQueue<AbstractObject>();
        this.executionService = executionService;
        setMailServer(mailServer);
        setNotifications(notifications);
        session = Session.getInstance(mailServer.getProperties());
    }

    public void record(AbstractObject object) {
        try {
            inQueue.put(object);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        running = true;

        while (!stopping || inQueue.size() > 0) {
            try {
                AbstractObject object = inQueue.poll(5, TimeUnit.SECONDS);
                if (object != null) {
                    executionService.save(object);
                    
                    if (object instanceof Execution) {
                        sendNotifications((Execution) object);
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        running = false;
    }

    private synchronized void sendNotifications(Execution execution) {
        List<Notification> notifications = notificationMap.get(execution.getStatus());
        if (notifications != null) {
            Transport transport = null;
            try {
                transport = session.getTransport(mailServer.getTransport());
                if (mailServer.isUseAuth()) {
                    transport.connect(mailServer.getUsername(), mailServer.getPassword());
                } else {
                    transport.connect();
                }
    
                for (Notification notification : notifications) {
                    String level = notification.getLevel();
                    String linkId = notification.getLinkId();
                    if (notification.isEnabled() && 
                            (level.equals(Notification.Level.GLOBAL.toString()) ||
                            (level.equals(Notification.Level.AGENT.toString()) && linkId.equals(execution.getAgentId())) ||
                            (level.equals(Notification.Level.DEPLOYMENT.toString()) && linkId.equals(execution.getDeploymentId())))) {
                        log.info("Sending notification " + notification.getName() + " of type " + notification.getNotifyType());
                        MimeMessage message = new MimeMessage(session);
                        Date date = new Date();
                        message.setRecipients(RecipientType.BCC, notification.getRecipients());
                        message.setSentDate(date);
                        
                        Map<String, String> prop = new HashMap<String, String>();
                        prop.put("agent", execution.getAgentName());
                        prop.put("deployment", execution.getDeploymentName());
                        prop.put("flow", execution.getFlowName());
                        prop.put("host", execution.getHostName());
                        prop.put("date", DateFormatUtils.format(date, DATE_FORMAT));
                        prop.put("time", DateFormatUtils.format(date, TIME_FORMAT));
                        
                        message.setSubject(FormatUtils.replaceTokens(notification.getSubject(), prop, true));
                        message.setText(FormatUtils.replaceTokens(notification.getMessage(), prop, true));
                        transport.sendMessage(message, message.getAllRecipients());
                    }
                }
            } catch (MessagingException e) {
                log.error("Failure while sending mail notification", e);
            } finally {
                try {
                    transport.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void shutdown() {
        this.stopping = true;
        
        while (this.running) {
            AppUtils.sleep(10);
        }
    }
    
    public synchronized void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    public synchronized void setNotifications(List<Notification> notifications) {
        notificationMap = new HashMap<String, List<Notification>>();
        for (Notification notification : notifications) {
            if (notification.getEventType().equals(Notification.EventType.FLOW_START.toString())) {
                addNotification(ExecutionStatus.RUNNING, notification);
            } else if (notification.getEventType().equals(Notification.EventType.FLOW_END.toString())) {
                addNotification(ExecutionStatus.DONE, notification);
                addNotification(ExecutionStatus.CANCELLED, notification);
                addNotification(ExecutionStatus.ABANDONED, notification);
                addNotification(ExecutionStatus.ERROR, notification);
            } else if (notification.getEventType().equals(Notification.EventType.FLOW_ERROR.toString())) {
                addNotification(ExecutionStatus.ERROR, notification);
            }
        }
    }

    private void addNotification(ExecutionStatus executionStatus, Notification notification) {
        String status = executionStatus.toString();
        List<Notification> list = notificationMap.get(status);
        if (list == null) {
            list = new ArrayList<Notification>();
            notificationMap.put(status, list);
        }
        list.add(notification);
    }

}
