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
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class AwsSqsWriter extends AbstractComponentRuntime {

    public final static String AWSSQS_WRITER_REGION_ATTRIBUTE = "awssqs.writer.region.attribute";
    public final static String AWSSQS_WRITER_QUEUEURL_ATTRIBUTE = "awssqs.writer.queueurl.attribute";
    public final static String AWSSQS_WRITER_MESSAGEGROUPID_ATTRIBUTE = "awssqs.writer.messagegroupid.attribute";
    
    private SqsClient sqsClient;
    
    /* settings */
    String queueUrl;
    String messageGroupId;

    @Override
    public void start() {    			
        TypedProperties properties = getTypedProperties();

        queueUrl = properties.get(AWSSQS_WRITER_QUEUEURL_ATTRIBUTE);
        messageGroupId = properties.get(AWSSQS_WRITER_MESSAGEGROUPID_ATTRIBUTE);
        
    	sqsClient = SqsClient.builder()
    			.region(Region.of(properties.get(AWSSQS_WRITER_REGION_ATTRIBUTE)))
    			.build();
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

	@Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
        	List<String> inputMessages = ((TextMessage) inputMessage).getPayload();
        	ArrayList<String> outputMessages = new ArrayList<String>();
            
        	outputMessages.add("***********************");
        	outputMessages.add("Queue URL: " + queueUrl);
        	outputMessages.add("Message Group ID: " + messageGroupId);
        	outputMessages.add("***********************");
        	outputMessages.addAll(inputMessages);
        	outputMessages.add("***********************");
        	
        	ListQueuesRequest request = ListQueuesRequest.builder().build();
            ListQueuesResponse response = sqsClient.listQueues(request);

            System.out.println("Queues available:");
            for (String url : response.queueUrls()) {
            	outputMessages.add(url);
            }
            
        	outputMessages.add("***********************");
        	
        	SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
        			.queueUrl(queueUrl)
        			.messageGroupId(messageGroupId)
        			.messageBody(inputMessages.get(0))
        			.build();
        	
        	sqsClient.sendMessage(sendMessageRequest);

            callback.sendTextMessage(null, outputMessages);
        }
    }
}
