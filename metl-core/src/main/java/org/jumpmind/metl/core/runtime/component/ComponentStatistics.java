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
package org.jumpmind.metl.core.runtime.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComponentStatistics {

    private Map<Integer, Integer> numberInboundMessages = new ConcurrentHashMap<>();
    private Map<Integer, Integer> numberOutboundMessages = new ConcurrentHashMap<>();
    private Map<Integer, Integer> numberEntitiesProcessed = new ConcurrentHashMap<>();
    private Map<Integer, Integer> numberInboundPayload = new ConcurrentHashMap<>();
    private Map<Integer, Integer> numberOutboundPayload = new ConcurrentHashMap<>();
    private Map<Integer, Long> timeSpentInHandle = new ConcurrentHashMap<>();
    private Map<Integer, Long> timeSpentWaiting = new ConcurrentHashMap<>();
    
    
    public long getTimeSpentInHandle(int thread) {
        Long number = timeSpentInHandle.get(thread);
        return number != null ? number : 0;
    }
    
    public long getTimeSpentWaiting(int thread) {
        Long number = timeSpentWaiting.get(thread);
        return number != null ? number : 0;
    }
    
    public void incrementTimeSpentInHandle(int thread, long amount) {
        if (amount > 0) {
            Long number = timeSpentInHandle.get(thread);
            if (number != null) {
                number = number + amount;
            } else {
                number = amount;
            }
            timeSpentInHandle.put(thread, number);
        }
    }

    public void incrementTimeSpentWaiting(int thread, long amount) {
        if (amount > 0) {
            Long number = timeSpentWaiting.get(thread);
            if (number != null) {
                number = number + amount;
            } else {
                number = amount;
            }
            timeSpentWaiting.put(thread, number);
        }
    }

    public int getNumberInboundMessages(int thread) {
        Integer number = numberInboundMessages.get(thread);
        return number != null ? number : 0;
    }

    public void setNumberInboundMessages(int thread, int numberInboundMessages) {
        this.numberInboundMessages.put(thread, numberInboundMessages);
    }

    public void incrementInboundMessages(int thread) {
        this.numberInboundMessages.put(thread, getNumberInboundMessages(thread)+1);
    }

    public void setNumberOutboundMessages(int thread, int numberOutboundMessages) {
        this.numberOutboundMessages.put(thread, numberOutboundMessages);
    }

    public int getNumberOutboundMessages(int thread) {
        Integer number = numberOutboundMessages.get(thread);
        return number != null ? number : 0;
    }

    public void incrementOutboundMessages(int thread) {
        this.numberOutboundMessages.put(thread, getNumberOutboundMessages(thread)+1);
    }

    public void setNumberEntitiesProcessed(int thread, int numberEntitiesProcessed) {
        this.numberEntitiesProcessed.put(thread, getNumberEntitiesProcessed(thread)+1);
    }

    public int getNumberEntitiesProcessed(int thread) {
        Integer number = numberEntitiesProcessed.get(thread);
        return number != null ? number : 0;
    }

    public void incrementNumberEntitiesProcessed(int thread) {
        this.numberEntitiesProcessed.put(thread, getNumberEntitiesProcessed(thread)+1);
    }

    public void incrementNumberEntitiesProcessed(int thread, int count) {
        this.numberEntitiesProcessed.put(thread, getNumberEntitiesProcessed(thread)+count);
    }
    
    public void setNumberInboundPayload(int thread, int numberInboundPayload) {
        this.numberInboundPayload.put(thread, getNumberInboundPayload(thread)+1);
    }

    public int getNumberInboundPayload(int thread) {
        Integer number = numberInboundPayload.get(thread);
        return number != null ? number : 0;
    }

    public void incrementNumberInboundPayload(int thread) {
        this.numberInboundPayload.put(thread, getNumberInboundPayload(thread)+1);
    }

    public void incrementNumberInboundPayload(int thread, int count) {
        this.numberInboundPayload.put(thread, getNumberInboundPayload(thread)+count);
    }
    
    public void setNumberOutboundPayload(int thread, int numberOutboundPayload) {
        this.numberOutboundPayload.put(thread, getNumberOutboundPayload(thread)+1);
    }

    public int getNumberOutboundPayload(int thread) {
        Integer number = numberOutboundPayload.get(thread);
        return number != null ? number : 0;
    }

    public void incrementNumberOutboundPayload(int thread) {
        this.numberOutboundPayload.put(thread, getNumberOutboundPayload(thread)+1);
    }

    public void incrementNumberOutboundPayload(int thread, int count) {
        this.numberOutboundPayload.put(thread, getNumberOutboundPayload(thread)+count);
    }
}
