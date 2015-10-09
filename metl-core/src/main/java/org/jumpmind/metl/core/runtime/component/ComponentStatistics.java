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

import java.util.HashMap;
import java.util.Map;

public class ComponentStatistics {

    private Map<Integer, Integer> numberInboundMessages = new HashMap<>();
    private Map<Integer, Integer> numberOutboundMessages = new HashMap<>();
    private Map<Integer, Integer> numberEntitiesProcessed = new HashMap<>();

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
}
