package org.jumpmind.metl.core.runtime.resource;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jumpmind.properties.TypedProperties;

public class JMSJndiTopicDirectory extends AbstractDirectory {

    TypedProperties properties;

    TopicConnection connection;

    TopicSession session;

    TopicPublisher producer;

    public JMSJndiTopicDirectory(TypedProperties properties) throws JMSException, NamingException {
        this.properties = properties;

        Hashtable<String, String> env = new Hashtable<String, String>();
        {
            env.put(Context.INITIAL_CONTEXT_FACTORY, properties.get(JMS.SETTING_INITIAL_CONTEXT_FACTORY));
            env.put(Context.PROVIDER_URL, properties.get(JMS.SETTING_PROVIDER_URL));
            String principal = properties.get(JMS.SETTING_SECURITY_PRINCIPAL);
            if (isNotBlank(principal)) {
                env.put(Context.SECURITY_PRINCIPAL, principal);
            }
            String credentials = properties.get(JMS.SETTING_SECURITY_CREDENTIALS);
            if (isNotBlank(credentials)) {
                env.put(Context.SECURITY_CREDENTIALS, credentials);
            }
        }

        Context ctx = new InitialContext(env);

        TopicConnectionFactory cf = (TopicConnectionFactory) ctx.lookup(properties.get(JMS.SETTING_CONNECTION_FACTORY_NAME));

        connection = cf.createTopicConnection();

        session = (TopicSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic topic = (Topic) ctx.lookup(properties.get(JMS.SETTING_TOPIC_NAME));

        producer = session.createPublisher(topic);

        connection.start();

    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        return new CloseableOutputStream();
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession) {
        return new CloseableOutputStream();
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
            }
        }
    }

    @Override
    public void connect() {
    }

    class CloseableOutputStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            super.close();
            String text = new String(toByteArray());
            try {
                String msgType = properties.get(JMS.SETTING_MESSAGE_TYPE, JMS.MSG_TYPE_TEXT);
                Message jmsMsg = null;
                if (JMS.MSG_TYPE_TEXT.equals(msgType)) {
                    jmsMsg = session.createTextMessage(text);
                } else if (JMS.MSG_TYPE_BYTES.equals(msgType)) {
                    BytesMessage msg = session.createBytesMessage();
                    msg.writeBytes(text.getBytes());
                    jmsMsg = msg;
                } else if (JMS.MSG_TYPE_OBJECT.equals(msgType)) {
                    ObjectMessage msg = session.createObjectMessage();
                    msg.setObject(text);
                    jmsMsg = msg;
                } else if (JMS.MSG_TYPE_MAP.equals(msgType)) {
                    String keyName = properties.get(JMS.SETTING_MESSAGE_TYPE_MAP_VALUE, "Payload");
                    MapMessage msg = session.createMapMessage();
                    msg.setString(keyName, text);
                    jmsMsg = msg;
                }
                
                if (jmsMsg != null) {
                    String jmsType = properties.get(JMS.SETTING_MESSAGE_JMS_TYPE);
                    if (isNotBlank(jmsType)) {
                        jmsMsg.setJMSType(jmsType);
                    }
                    producer.publish(jmsMsg);
                }
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
