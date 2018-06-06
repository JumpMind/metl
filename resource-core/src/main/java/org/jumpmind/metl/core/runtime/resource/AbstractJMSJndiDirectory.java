/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.resource;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Hashtable;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicPublisher;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractJMSJndiDirectory extends AbstractDirectory {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected TypedProperties properties;

    protected Connection connection;

    protected Session session;

    protected Context context;

    protected MessageConsumer consumer;

    protected MessageProducer producer;

    protected Message lastMessage;

    public AbstractJMSJndiDirectory(TypedProperties properties) throws JMSException, NamingException {
        this.properties = properties;
        initialize();
    }

    protected void close(Object toClose) {
        if (toClose != null) {
            try {
                Method method = toClose.getClass().getMethod("close");
                if (method != null) {
                    try {
                        method.setAccessible(true);
                        method.invoke(toClose);
                    } catch (IllegalAccessException e) {
                        throw e;
                    }
                } else {
                    String msg = String.format("Could not find a close method to call on the class: %s", toClose.getClass().getName());
                    throw new IllegalAccessException(msg);
                }
            } catch (NoSuchMethodError e) {
                throw e;
            } catch (Exception ex) {
            }
        }
    }

    protected void initProducer() {
        if (producer == null) {
            producer = createProducer();
        }
    }

    protected void initConsumer() {
        if (consumer == null) {
            consumer = createConsumer();
        }
    }

    protected void initialize() throws JMSException, NamingException {
        if (connection == null) {
            try {
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

                context = new InitialContext(env);
                connection = createConnection(context);
                session = createSession(connection);
                connection.start();

            } catch (RuntimeException e) {
                throw e;
            } catch (JMSException e) {
                throw e;
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    abstract protected MessageProducer createProducer();

    abstract protected Connection createConnection(Context context) throws JMSException, NamingException;

    abstract protected Session createSession(Connection connection) throws JMSException, NamingException;

    abstract protected MessageConsumer createConsumer();

    @Override
    public boolean supportsInputStream() {
        return true;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        return getInputStream(relativePath, mustExist, false);
    }

    public static String getPayload(Message message, String mapTypeKeyName) throws JMSException {
        StringBuilder builder = new StringBuilder();
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();
            if (isNotBlank(text)) {
                builder.append(text);
            }
        } else if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            String text = mapMessage.getString(mapTypeKeyName);
            if (isNotBlank(text)) {
                builder.append(text);
            }
        } else if (message instanceof ObjectMessage) {
            ObjectMessage objMessage = (ObjectMessage) message;
            Object obj = objMessage.getObject();
            if (obj != null) {
                builder.append(obj.toString());
            }
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            long length = bytesMessage.getBodyLength();
            byte[] bytes = new byte[(int) length];
            bytesMessage.readBytes(bytes, (int) length);
        }
        return builder.toString();
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
        try {
            initConsumer();
            String payload = null;
            try {
                Message message = consumer.receive(properties.getInt(JMS.SETTING_WAIT_FOR_MESSAGE_TIMEOUT_MS, 5000));
                if (message != null) {
                    lastMessage = message;
                    String keyName = properties.get(JMS.SETTING_MESSAGE_TYPE_MAP_VALUE, "Payload");
                    payload = getPayload(message, keyName);
                }

            } finally {
                if (closeSession) {
                    AbstractJMSJndiDirectory.this.close();
                }
            }

            if (payload != null && payload.length() > 0) {
                return new ByteArrayInputStream(payload.getBytes());
            } else {
                return null;
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        return new CloseableOutputStream(relativePath, true);
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession, boolean append) {
        return new CloseableOutputStream(relativePath, closeSession);
    }

    public void register(MessageListener listener) {
        try {
            initialize();
            initConsumer();
            consumer.setMessageListener(listener);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
    }

    @Override
    public void close(boolean success) {
        if (success && properties.get(JMS.SETTING_ACK_TYPE, JMS.ACK_TYPE_IMMEDIATE).equals(JMS.ACK_TYPE_ON_FLOW_COMPLETE)
                && lastMessage != null) {
            try {
                lastMessage.acknowledge();
            } catch (JMSException e) {
                log.error("The call to acknowledge failed", e);
            }
        }
        close(producer);
        producer = null;
        close(consumer);
        consumer = null;
        close(session);
        session = null;
        close(connection);
        connection = null;
    }

    @Override
    public void connect() {
    }

    class CloseableOutputStream extends ByteArrayOutputStream {

        String relativePath;

        boolean closeSession;

        public CloseableOutputStream(String relativePath, boolean closeSession) {
            this.relativePath = relativePath;
            initProducer();
        }

        @Override
        public void close() throws IOException {
            super.close();
            String text = new String(toByteArray());
            try {
                initialize();
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
                        if (isNotBlank(relativePath)) {
                            jmsType = FormatUtils.replaceToken(jmsType, "relativePath", relativePath, true);
                        }
                        jmsMsg.setJMSType(jmsType);
                    }
                    if (producer instanceof TopicPublisher) {
                        TopicPublisher pub = (TopicPublisher) producer;
                        pub.publish(jmsMsg);
                    } else {
                        producer.send(jmsMsg);
                    }
                }
            } catch (Exception e) {
                AbstractJMSJndiDirectory.this.close();
                throw new RuntimeException(e);
            } finally {
                if (closeSession) {
                    AbstractJMSJndiDirectory.this.close();
                }
            }
        }
    }

}
