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

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Union extends AbstractComponentRuntime {

    public static final String TYPE = "Union";
        
    ArrayList<EntityData> dataToSend = new ArrayList<EntityData>();

    @Override
    public void start() {
    }
        
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }
    
    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {    	
        if (inputMessage instanceof EntityDataMessage) {
            ArrayList<EntityData> payload = ((EntityDataMessage)inputMessage).getPayload();
            dataToSend.addAll(payload);
            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber, payload.size());
        }

        if (unitOfWorkBoundaryReached && dataToSend.size() != 0) {
            callback.sendEntityDataMessage(null, dataToSend);
            dataToSend.clear();
        }
    }

}
