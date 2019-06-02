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
    String resourceType;

    public JMSJndiTopicDirectory(TypedProperties properties, String resourceType) throws JMSException, NamingException {
        super(properties);
        this.resourceType = resourceType;
    }

    @Override
    protected void initialize() throws JMSException, NamingException {
        super.initialize();
        if (resourceType != null && resourceType.equalsIgnoreCase(JMS.RESOURCE_TYPE_SUBSCRIBE)) {
            MessageConsumer consumer = createConsumer();
            close(consumer);
        }
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
        int ackSetting = Session.AUTO_ACKNOWLEDGE;
        if (properties.get(JMS.SETTING_ACK_TYPE, JMS.ACK_TYPE_IMMEDIATE).equals(JMS.ACK_TYPE_ON_FLOW_COMPLETE)) {
            ackSetting = Session.CLIENT_ACKNOWLEDGE;
        }
        return connection.createSession(false, ackSetting);
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
