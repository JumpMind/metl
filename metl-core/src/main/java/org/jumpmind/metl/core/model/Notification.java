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
package org.jumpmind.metl.core.model;

public class Notification extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    public enum NotificationLevel { 
        GLOBAL, AGENT, DEPLOYMENT 
    }
    
    public enum NotifyType {
        MAIL
    }
    
    public enum EventType {
        FLOW_START, FLOW_END, FLOW_ERROR
    }
    
    static final EventType[] AGENT_EVENT_TYPES = { EventType.FLOW_START, EventType.FLOW_END, EventType.FLOW_ERROR };
    
    static final EventType[] DEPLOYMENT_EVENT_TYPES = { EventType.FLOW_START, EventType.FLOW_END, EventType.FLOW_ERROR };

    static final EventType[] GLOBAL_EVENT_TYPES = { EventType.FLOW_START, EventType.FLOW_END, EventType.FLOW_ERROR };
    
    String notificationLevel;
    
    String linkId;

    String name;
    
    String notifyType;

    String eventType;
    
    String subject;
    
    String recipients;
    
    String message;
    
    boolean enabled;

    public static EventType[] getEventTypesForLevel(String notificationLevel) {
        EventType[] eventTypes = null;
        if (notificationLevel.equals(NotificationLevel.GLOBAL.toString())) {
            eventTypes = GLOBAL_EVENT_TYPES;
        } else if (notificationLevel.equals(NotificationLevel.AGENT.toString())) {
            eventTypes = AGENT_EVENT_TYPES;
        } else if (notificationLevel.equals(NotificationLevel.DEPLOYMENT.toString())) {
            eventTypes = Notification.DEPLOYMENT_EVENT_TYPES;
        }
        return eventTypes;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getNotificationLevel() {
        return notificationLevel;
    }

    public void setNotificationLevel(String notificationLevel) {
        this.notificationLevel = notificationLevel;
    }

    public String getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(String notifyType) {
        this.notifyType = notifyType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }    
    
}
