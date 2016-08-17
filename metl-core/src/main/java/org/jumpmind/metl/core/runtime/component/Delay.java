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

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.AppUtils;

public class Delay extends AbstractComponentRuntime {

    public static final String TYPE = "Delay";
    
    public final static String DELAY_TIME = "delay.in.ms";
    
    String runWhen = PER_UNIT_OF_WORK;
    
    long delay = 1000;

    @Override
    public void start() {        
        delay = getComponent().getLong(DELAY_TIME, 1000l);
        runWhen = getComponent().get(RUN_WHEN, PER_UNIT_OF_WORK);
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }
    
	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			AppUtils.sleep(delay);
		}
		callback.forward(inputMessage);
	}
}
