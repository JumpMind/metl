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
        int ackSetting = Session.AUTO_ACKNOWLEDGE;
        if (properties.get(JMS.SETTING_ACK_TYPE, JMS.ACK_TYPE_IMMEDIATE).equals(JMS.ACK_TYPE_ON_FLOW_COMPLETE)) {
            ackSetting = Session.CLIENT_ACKNOWLEDGE;
        }
        return (QueueSession) connection.createSession(false, ackSetting);
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
