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

import java.util.HashMap;
import java.util.Map;

import javax.jms.MessageListener;
import javax.naming.Context;

public class JMS extends AbstractResourceRuntime implements ISubscribe {

    public static final String TYPE = "JMS";

    public static final String CREATE_MODE_JNDI = "JNDI";

    public static final String TYPE_TOPIC = "Topic";
    
    public static final String TYPE_QUEUE = "Queue";

    public static final String MSG_TYPE_TEXT = "Text";

    public static final String MSG_TYPE_BYTES = "Byte";

    public static final String MSG_TYPE_OBJECT = "Object";

    public static final String MSG_TYPE_MAP = "Map";
    
    public static final String ACK_TYPE_ON_FLOW_COMPLETE = "On Flow Complete";
    
    public static final String ACK_TYPE_IMMEDIATE = "Immediate";

    public static final String SETTING_CREATE_MODE = "create.mode";

    public static final String SETTING_TYPE = "jms.type";

    public static final String SETTING_INITIAL_CONTEXT_FACTORY = Context.INITIAL_CONTEXT_FACTORY;

    public static final String SETTING_PROVIDER_URL = Context.PROVIDER_URL;

    public static final String SETTING_SECURITY_PRINCIPAL = Context.SECURITY_PRINCIPAL;

    public static final String SETTING_SECURITY_CREDENTIALS = Context.SECURITY_CREDENTIALS;

    public static final String SETTING_CONNECTION_FACTORY_NAME = "connection.factory.name";

    public static final String SETTING_QUEUE_NAME = "queue.name";
    
    public static final String SETTING_TOPIC_NAME = "topic.name";
    
    public static final String SETTING_DURABLE_SUBSCRIPTION_NAME = "durable.subscription.name";
    
    public static final String SETTING_CLIENT_ID = "client.id";    
    
    public static final String SETTING_MESSAGE_TYPE = "msg.type";

    public static final String SETTING_MESSAGE_TYPE_MAP_VALUE = "map.msg.key";
    
    public static final String SETTING_MESSAGE_JMS_TYPE = "msg.jms.type";
    
    public static final String SETTING_ACK_TYPE = "ack.type";
    
    public static final String SETTING_WAIT_FOR_MESSAGE_TIMEOUT_MS = "wait.for.message.timeout.ms";
    
    public static final String RESOURCE_TYPE_SUBSCRIBE = "JMS Subscribe";
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        AbstractJMSJndiDirectory streamableResource = null;
        try {
            String createMode = resourceRuntimeSettings.get(SETTING_CREATE_MODE);
            if (CREATE_MODE_JNDI.equals(createMode)) {
                String type = resourceRuntimeSettings.get(SETTING_TYPE);
                if (TYPE_TOPIC.equals(type)) {
                    streamableResource = new JMSJndiTopicDirectory(resourceRuntimeSettings, this.getResource().getType());
                } else if (TYPE_QUEUE.equals(type)) {
                    streamableResource = new JMSJndiQueueDirectory(resourceRuntimeSettings);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to start JMS resource", e);
        }        
        return (T) streamableResource;
    }
    
    Map<MessageListener, AbstractJMSJndiDirectory> listeners = new HashMap<>();
    
    @Override
    public void start(MessageListener listener) {
        AbstractJMSJndiDirectory directory = reference();
        if (this.getResource().getType().equalsIgnoreCase(RESOURCE_TYPE_SUBSCRIBE)) {
            log.info("About to register JMS listener");
            directory.register(listener);
            listeners.put(listener, directory);
        }
    }
    
    @Override
    public void stop(MessageListener listener) {
        AbstractJMSJndiDirectory directory = listeners.remove(listener);
        if (directory != null) {
            log.info("About to shutdown JMS listener");
            directory.close();            
        }
    }
    
    @Override
    public void stop() {
        log.error("Stop called!");
    }
    
}
