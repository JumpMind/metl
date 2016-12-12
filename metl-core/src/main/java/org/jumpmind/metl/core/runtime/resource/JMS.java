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

import javax.naming.Context;

import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;
import org.jumpmind.metl.core.plugin.XMLSetting.Type;
import org.jumpmind.properties.TypedProperties;

@ResourceDefinition(typeName = JMS.TYPE, resourceCategory = ResourceCategory.STREAMABLE)
public class JMS extends AbstractResourceRuntime {

    public static final String TYPE = "JMS";

    public static final String CREATE_MODE_JNDI = "JNDI";

    public static final String TYPE_TOPIC = "Topic";
    
    public static final String TYPE_QUEUE = "Queue";

    public static final String MSG_TYPE_TEXT = "Text";

    public static final String MSG_TYPE_BYTES = "Byte";

    public static final String MSG_TYPE_OBJECT = "Object";

    public static final String MSG_TYPE_MAP = "Map";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.CHOICE,
            label = "Create Mode",
            choices = { CREATE_MODE_JNDI },
            defaultValue = CREATE_MODE_JNDI)
    public static final String SETTING_CREATE_MODE = "create.mode";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.CHOICE,
            label = "JMS Type",
            choices = { TYPE_TOPIC, TYPE_QUEUE },
            defaultValue = TYPE_TOPIC)
    public static final String SETTING_TYPE = "jms.type";

    @SettingDefinition(
            type = Type.TEXT,
            order = 50,
            required = false,
            label = Context.INITIAL_CONTEXT_FACTORY,
            defaultValue = "org.apache.activemq.jndi.ActiveMQInitialContextFactory")
    public static final String SETTING_INITIAL_CONTEXT_FACTORY = Context.INITIAL_CONTEXT_FACTORY;

    @SettingDefinition(type = Type.TEXT, order = 60, required = false, label = Context.PROVIDER_URL, defaultValue = "vm://localhost?broker.persistent=false")
    public static final String SETTING_PROVIDER_URL = Context.PROVIDER_URL;

    @SettingDefinition(type = Type.TEXT, order = 70, required = false, label = Context.SECURITY_PRINCIPAL)
    public static final String SETTING_SECURITY_PRINCIPAL = Context.SECURITY_PRINCIPAL;

    @SettingDefinition(type = Type.PASSWORD, order = 80, required = false, label = Context.SECURITY_CREDENTIALS)
    public static final String SETTING_SECURITY_CREDENTIALS = Context.SECURITY_CREDENTIALS;

    @SettingDefinition(type = Type.TEXT, order = 100, required = false, label = "Connection Factory Name", defaultValue = "ConnectionFactory")
    public static final String SETTING_CONNECTION_FACTORY_NAME = "connection.factory.name";

    @SettingDefinition(type = Type.TEXT, order = 110, required = false, label = "Queue Name", defaultValue = "dynamicQueues/foo.bar")
    public static final String SETTING_QUEUE_NAME = "queue.name";
    
    @SettingDefinition(type = Type.TEXT, order = 120, required = false, label = "Topic Name", defaultValue = "dynamicTopics/foo.bar")
    public static final String SETTING_TOPIC_NAME = "topic.name";
    
    @SettingDefinition(type = Type.TEXT, order = 130, required = false, label = "Durable Subscription Name")
    public static final String SETTING_DURABLE_SUBSCRIPTION_NAME = "durable.subscription.name";
    
    @SettingDefinition(type = Type.TEXT, order = 135, required = false, label = "Client ID")
    public static final String SETTING_CLIENT_ID = "client.id";    
    
    @SettingDefinition(
            order = 150,
            required = true,
            type = Type.CHOICE,
            label = "Message Type",
            choices = { MSG_TYPE_TEXT, MSG_TYPE_BYTES, MSG_TYPE_MAP, MSG_TYPE_OBJECT },
            defaultValue = MSG_TYPE_TEXT)
    public static final String SETTING_MESSAGE_TYPE = "msg.type";

    @SettingDefinition(order = 160, required = false, type = Type.TEXT, label = "Map Message Key", defaultValue = "Payload")
    public static final String SETTING_MESSAGE_TYPE_MAP_VALUE = "map.msg.key";
    
    @SettingDefinition(order = 170, required = false, type = Type.TEXT, label = "Message JMS Type")
    public static final String SETTING_MESSAGE_JMS_TYPE = "msg.jms.type";    

    IDirectory streamableResource;

    TypedProperties properties;

    @Override
    protected void start(TypedProperties properties) {
        this.properties = properties;
        try {
            String createMode = properties.get(SETTING_CREATE_MODE);
            if (CREATE_MODE_JNDI.equals(createMode)) {
                String type = properties.get(SETTING_TYPE);
                if (TYPE_TOPIC.equals(type)) {
                    streamableResource = new JMSJndiTopicDirectory(properties);
                } else if (TYPE_QUEUE.equals(type)) {
                    streamableResource = new JMSJndiQueueDirectory(properties);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (streamableResource != null) {
            streamableResource.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) streamableResource;
    }
}
