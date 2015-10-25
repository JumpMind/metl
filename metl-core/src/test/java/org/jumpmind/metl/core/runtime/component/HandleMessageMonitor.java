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
import java.util.List;

import org.jumpmind.metl.core.runtime.Message;

public class HandleMessageMonitor {
	private int shutdownMessageCount;
	private int startupMessageCount;
	private List<Message> messages;
	private List<String> targetStepIds = new ArrayList<String>();
	private boolean xmlPayload;
	
	public void incrementShutdownMessageCount() {
		this.shutdownMessageCount++;
	}
	
	public void incrementStartupMessageCount() {
		this.startupMessageCount++;
	}
	
	public int getShutdownMessageCount() {
		return shutdownMessageCount;
	}
	public void setShutdownMessageCount(int shutdownMessageCount) {
		this.shutdownMessageCount = shutdownMessageCount;
	}
	public int getStartupMessageCount() {
		return startupMessageCount;
	}
	public void setStartupMessageCount(int startupMessageCount) {
		this.startupMessageCount = startupMessageCount;
	}
	public int getSendMessageCount() {
		return messages != null ? messages.size() : 0;
	}
	
	public List<String> getTargetStepIds() {
		return targetStepIds;
	}
	public void setTargetStepIds(List<String> targetStepIds) {
		this.targetStepIds = targetStepIds;
	}

	boolean isXmlPayload() {
		return xmlPayload;
	}

	void setXmlPayload(boolean xmlPayload) {
		this.xmlPayload = xmlPayload;
	}

	List<Message> getMessages() {
		if (messages == null) {
			messages = new ArrayList<Message>();
		}
		return messages;
	}

	void setMessages(List<Message> messages) {
		this.messages = messages;
	}
	
	
	
	
}
