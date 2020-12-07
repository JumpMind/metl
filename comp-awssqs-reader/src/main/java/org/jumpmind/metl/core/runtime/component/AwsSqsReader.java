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

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class AwsSqsReader extends AbstractComponentRuntime {

    public final static String AWSSQS_READER_REGION_ATTRIBUTE = "awssqs.reader.region.attribute";
    public final static String AWSSQS_READER_QUEUEURL_ATTRIBUTE = "awssqs.reader.queueurl.attribute";
    public final static String AWSSQS_READER_NUMBEROFMESSAGES_ATTRIBUTE = "awssqs.reader.numberofmessages.attribute";
    
    private SqsClient sqsClient;
    
    /* settings */
    String queueUrl;
    int numberOfMessages;

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();

        queueUrl = properties.get(AWSSQS_READER_QUEUEURL_ATTRIBUTE);
        numberOfMessages = Integer.parseInt(properties.get(AWSSQS_READER_NUMBEROFMESSAGES_ATTRIBUTE));

        sqsClient = SqsClient.builder()
                .region(Region.of(properties.get(AWSSQS_READER_REGION_ATTRIBUTE)))
                .build();
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

	@Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            ArrayList<String> outputMessages = new ArrayList<String>();
            
            outputMessages.add("***********************");
            outputMessages.add("AWS SQS READER");
            outputMessages.add("***********************");
            outputMessages.add("Queue URL: " + queueUrl);
            outputMessages.add("Number Of Messages: " + numberOfMessages);
            outputMessages.add("***********************");

            for (int i = 0; i < numberOfMessages; i++) {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .build();

                ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);

                for (software.amazon.awssdk.services.sqs.model.Message message : response.messages()) {
                    outputMessages.add(message.toString());
                    
                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build();

                    sqsClient.deleteMessage(deleteRequest);
                }
            }

            callback.sendTextMessage(null, outputMessages);
        }
    }
}
