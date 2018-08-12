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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class Gate extends AbstractComponentRuntime {

    public static final String TYPE = "Gate";
    
    public final static String SOURCE_STEP = "gate.control.source.step";

    public final static String SETTING_FORCE_GATE_OPEN = "force.gate.open";

    boolean gateOpened = false;

    boolean forceGateOpen = false;

    String gateControlSourceStepId;

    List<Message> queuedWhileWaitingForGateController = new ArrayList<Message>();
    
    ControlMessage lastControlMessageReceived;
    
    @Override
    public void start() {
    	gateOpened = false;
        TypedProperties typedProperties = getTypedProperties();
        gateControlSourceStepId = typedProperties.get(SOURCE_STEP); 
        forceGateOpen = typedProperties.is(SETTING_FORCE_GATE_OPEN, forceGateOpen);
        
        if (isBlank(gateControlSourceStepId) || getFlow().findFlowStepWithId(gateControlSourceStepId) == null) {
            throw new IllegalStateException("The gate control source must be specified");
        }
    }
    
    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (gateControlSourceStepId.equals(inputMessage.getHeader().getOriginatingStepId())) {

            gateOpened = !gateOpened && inputMessage instanceof ControlMessage;

            if (gateOpened) {
                info("The gate was just opened.  Releasing %d queue'd messages and every message from sources after this", queuedWhileWaitingForGateController.size());
                Iterator<Message> messages = queuedWhileWaitingForGateController.iterator();
                while (messages.hasNext()) {
                    Message message = messages.next();
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                    callback.forward(message.getHeader(), message);
                }
            }
        } else if (!gateOpened && !(inputMessage instanceof ControlMessage)) {
        	queuedWhileWaitingForGateController.add(inputMessage);
        } else if (gateOpened && !(inputMessage instanceof ControlMessage)) {
        	getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
        	callback.forward(inputMessage);        	
        } else if (unitOfWorkBoundaryReached && !gateOpened && forceGateOpen) {
            Iterator<Message> messages = queuedWhileWaitingForGateController.iterator();
            while (messages.hasNext()) {
                Message message = messages.next();
                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                callback.forward(message.getHeader(), message);
            }
        } else if (unitOfWorkBoundaryReached && (inputMessage instanceof ControlMessage)) {
            callback.forward(inputMessage.getHeader(), inputMessage);
        }
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
