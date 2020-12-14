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

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SQSWriter extends AbstractComponentRuntime {

    public final static String TYPE = "SQS Writer";

    public final static String SQS_WRITER_QUEUE_URL = "sqs.writer.queue.url";
    public final static String SQS_WRITER_MESSAGE_GROUP_ID = "sqs.writer.message.group.id";

    /* settings */
    String queueUrl;
    String messageGroupId;

    @Override
    public void start() {
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("SQS queue resource must be defined");
        }
        TypedProperties properties = getTypedProperties();
        queueUrl = properties.get(SQS_WRITER_QUEUE_URL);
        messageGroupId = properties.get(SQS_WRITER_MESSAGE_GROUP_ID);

        if (queueUrl.endsWith(".fifo") && (messageGroupId == null || messageGroupId.isEmpty())) {
            throw new IllegalStateException("Message Group ID is required for FIFO queue.");
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            for (String input : ((TextMessage) inputMessage).getPayload()) { 
                if (messageGroupId == null || messageGroupId.isEmpty()) {
                    sendMessage(SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(input)
                            .build());
                } else {
                    sendMessage(SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageGroupId(messageGroupId)
                            .messageBody(input)
                            .build());
                }
            }
        }
    }

    private void sendMessage(SendMessageRequest request) {
        try {
            SqsClient client = (SqsClient)getResourceReference();
            client.sendMessage(request);
        } catch (Exception e) {
            throw new RuntimeException("SQS Send Message failed: " + e.getMessage());
        }
    }
}
