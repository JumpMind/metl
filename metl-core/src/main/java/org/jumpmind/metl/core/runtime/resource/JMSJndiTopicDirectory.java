package org.jumpmind.metl.core.runtime.resource;

import java.util.UUID;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jumpmind.properties.TypedProperties;

public class JMSJndiTopicDirectory extends AbstractJMSJndiDirectory {

    TopicPublisher producer;

    public JMSJndiTopicDirectory(TypedProperties properties) throws JMSException, NamingException {
        super(properties);
    }
    
    @Override
    protected void initialize() throws JMSException, NamingException {
        super.initialize();
        MessageConsumer consumer = createConsumer();
        close(consumer);
    }

    @Override
    protected Connection createConnection(Context context) throws JMSException, NamingException {
        TopicConnectionFactory cf = (TopicConnectionFactory) context.lookup(properties.get(JMS.SETTING_CONNECTION_FACTORY_NAME));
        Connection connection = cf.createTopicConnection();
        connection.setClientID(properties.get(JMS.SETTING_CLIENT_ID, UUID.randomUUID().toString()));
        return connection;
    }

    @Override
    protected Session createSession(Connection connection) throws JMSException, NamingException {
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    protected MessageProducer createProducer() {
        try {
            Topic topic = (Topic) context.lookup(properties.get(JMS.SETTING_TOPIC_NAME));
            return ((TopicSession) session).createPublisher(topic);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected MessageConsumer createConsumer() {
        try {            
            Topic topic = (Topic) context.lookup(properties.get(JMS.SETTING_TOPIC_NAME));
            return ((TopicSession) session).createDurableSubscriber(topic, properties.get(JMS.SETTING_DURABLE_SUBSCRIPTION_NAME));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String toString() {
        return String.format("JMS Topic: %s", properties.get(JMS.SETTING_TOPIC_NAME)); 
    }

}
