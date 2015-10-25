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
package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;

public class MessageBuilder {
	
	Message message;
	
	public MessageBuilder() {
		message = new Message("unitTest");
	}
	public MessageBuilder(String originatingStepId) {
		message = new Message(originatingStepId);
	}
	
	public MessageBuilder withPayload(ArrayList<EntityData> payload) {
		this.message.setPayload(payload);
		return this;
	}
	
	public MessageBuilder withPayloadString(ArrayList<String> payload) {
		this.message.setPayload(payload);
		return this;
	}
	
	public Message build() {
		return this.message;
	}

}
