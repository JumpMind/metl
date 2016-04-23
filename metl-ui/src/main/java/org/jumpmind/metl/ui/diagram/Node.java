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
package org.jumpmind.metl.ui.diagram;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Node implements Serializable {

    private static final long serialVersionUID = 1L;

    int height = 40;
    int width = 55;
    int x = 0;
    int y = 0;
    String outputLabel;
    String inputLabel;
    String text;    
    String name;
    String id = UUID.randomUUID().toString();
    List<String> targetNodeIds = new ArrayList<String>();
    boolean enabled = true;
    
    long messagesRecieved = 0;
    long messagesSent = 0;
    long entitiesProcessed = 0;
    String status = "READY";

	public Node(String text) {
        this.text = text;
    }
    
    public Node() {
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setTargetNodeIds(List<String> targetNodeIds) {
        this.targetNodeIds = targetNodeIds;
    }
    
    public List<String> getTargetNodeIds() {
        return targetNodeIds;
    }
    
    public String getInputLabel() {
        return inputLabel;
    }
    
    public void setInputLabel(String inputLabel) {
        this.inputLabel = inputLabel;
    }
    
    public String getOutputLabel() {
        return outputLabel;
    }
    
    public void setOutputLabel(String outputLabel) {
        this.outputLabel = outputLabel;
    }

    public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

    public long getMessagesRecieved() {
        return messagesRecieved;
    }

    public void setMessagesRecieved(long messagesRecieved) {
        this.messagesRecieved = messagesRecieved;
    }

    public long getMessagesSent() {
        return messagesSent;
    }

    public void setMessagesSent(long messagesSent) {
        this.messagesSent = messagesSent;
    }

    public long getEntitiesProcessed() {
        return entitiesProcessed;
    }

    public void setEntitiesProcessed(long entitiesProcessed) {
        this.entitiesProcessed = entitiesProcessed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
