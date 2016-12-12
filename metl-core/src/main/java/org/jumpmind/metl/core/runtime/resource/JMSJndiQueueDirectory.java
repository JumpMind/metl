package org.jumpmind.metl.core.runtime.resource;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jumpmind.properties.TypedProperties;

public class JMSJndiQueueDirectory extends AbstractJMSJndiDirectory {

    public JMSJndiQueueDirectory(TypedProperties properties) throws JMSException, NamingException {
        super(properties);
    }

    @Override
    protected Connection createConnection(Context context) throws JMSException, NamingException {
        QueueConnectionFactory cf = (QueueConnectionFactory) context.lookup(properties.get(JMS.SETTING_CONNECTION_FACTORY_NAME));
        return cf.createQueueConnection();
    }

    @Override
    protected Session createSession(Connection connection) throws JMSException, NamingException {       
        return (QueueSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    protected MessageProducer createProducer() {
        try {
            Queue queue = (Queue) context.lookup(properties.get(JMS.SETTING_QUEUE_NAME));
            return session.createProducer(queue);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected MessageConsumer createConsumer() {
        try {
            Queue queue = (Queue) context.lookup(properties.get(JMS.SETTING_QUEUE_NAME));
            return session.createConsumer(queue);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String toString() {
        return String.format("JMS Queue: %s", properties.get(JMS.SETTING_QUEUE_NAME)); 
    }

}
