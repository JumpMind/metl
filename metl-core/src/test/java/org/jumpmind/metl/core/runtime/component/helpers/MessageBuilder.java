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
	
	public MessageBuilder withKeyValue(String key, Object value, int entityPosition) {
		if (this.message.getPayload() == null) {
			this.message.setPayload(new ArrayList<EntityData>());
		}
		for (int i =  ((ArrayList<EntityData>) this.message.getPayload()).size(); i < entityPosition; i++) {
			((ArrayList<EntityData>) this.message.getPayload()).add(new EntityDataBuilder().build());
		}
		((ArrayList<EntityData>) this.message.getPayload()).get(entityPosition - 1).put(key, value);
		
		return this;
	}
	
	public MessageBuilder withKeyValue(String key, Object value) {
		return withKeyValue(key, value, 1);
	}
	
	public MessageBuilder withValue(String value) {
		if (this.message.getPayload() == null) {
			this.message.setPayload(new ArrayList<String>());
		}
		((ArrayList<String>) this.message.getPayload()).add(value);
		
		return this;
	}
	
	public Message build() {
		return this.message;
	}

	public static Message buildKV(String key, Object value) {
		return new MessageBuilder()
			.withPayload(new PayloadBuilder()
					.withRow(new EntityDataBuilder()
							.withKV(key, value)
			.build()).buildED()).build();
	}
}
