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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class SQSReader extends AbstractComponentRuntime {

    public final static String TYPE = "SQS Reader";
    public final static String MESSAGE_HEADER_KEY = "sqsMessageReceipt";

    public final static String SQS_READER_QUEUE_URL = "sqs.reader.queue.url";
    public final static String SQS_READER_MAX_MESSAGES_TO_READ = "sqs.reader.max.messages.to.read";
    public final static String SQS_READER_DELETE_WHEN = "sqs.reader.delete.when";
    public final static String SQS_READER_READ_UNTIL_QUEUE_EMPTY = "sqs.reader.read.until.queue.empty";
    public final static String SQS_READER_MAX_RESULTS_PER_READ = "sqs.reader.max.results.per.read";

    /* settings */
    String runWhen;
    String queueUrl;
    String deleteWhen;
    int maxMsgsToRead;
    int maxResultsPerRead;
    int messagesPerOutputMessage;
    boolean readUntilQueueEmpty;
    List<String> messageReceipts;

    @Override
    public void start() {
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("SQS queue resource must be defined");
        }

        TypedProperties properties = getTypedProperties();
        runWhen = properties.get(RUN_WHEN);
        queueUrl = properties.get(SQS_READER_QUEUE_URL);
        maxMsgsToRead = properties.getInt(SQS_READER_MAX_MESSAGES_TO_READ);
        maxResultsPerRead = properties.getInt(SQS_READER_MAX_RESULTS_PER_READ);
        deleteWhen = properties.getProperty(SQS_READER_DELETE_WHEN);
        readUntilQueueEmpty = Boolean.valueOf(properties.getProperty(SQS_READER_READ_UNTIL_QUEUE_EMPTY));
        messageReceipts = new ArrayList<String>();

        if (maxMsgsToRead < 0) {
            throw new MisconfiguredException("\"Max Messages to Read\" must be a 0 or a positive number");
        }
        
        if (maxMsgsToRead > 0 && readUntilQueueEmpty) {
            throw new MisconfiguredException("\"Max Messages to Read\" and \"Read Until Queue Empty\" cannot be set in conjunction.");
        }

        if (maxResultsPerRead < 1 || maxResultsPerRead > 10) {
            throw new MisconfiguredException("\"Max Results Per Read\" must be between 1 and 10.");
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (PER_MESSAGE.equals(runWhen) && (!(inputMessage instanceof ControlMessage) || context.isStartStep())
                || (PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)) {

            SqsClient client = (SqsClient)getResourceReference();
            int messagesRead = 0;

            while (readUntilQueueEmpty || messagesRead < maxMsgsToRead) {
                software.amazon.awssdk.services.sqs.model.Message message = readMessage(client);

                if (message == null) return;

                messagesRead++;
                Map<String,Serializable> header = new LinkedHashMap<>();
                header.put(MESSAGE_HEADER_KEY, message.receiptHandle());
                callback.sendTextMessage(header, message.body());
            }
        }
    }
    
    @Override
    public void flowCompleted(boolean cancelled) {
        if ("ON FLOW COMPLETION".equals(deleteWhen) && !cancelled) {
            SqsClient client = (SqsClient)getResourceReference();
            
            messageReceipts.forEach( messageReceipt -> {
                deleteMessage(client, messageReceipt);
            });
        }
    }

    private software.amazon.awssdk.services.sqs.model.Message readMessage(SqsClient client) {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxResultsPerRead)
                    .build();

            ReceiveMessageResponse response = client.receiveMessage(request);

            for (software.amazon.awssdk.services.sqs.model.Message message : response.messages()) {
                if (message != null) {
                    messageReceipts.add(message.receiptHandle());
                    if ("ON READ".equals(deleteWhen)) {
                        deleteMessage(client, message.receiptHandle());
                    }
                    return message;
                }
            }

            return null;
        } catch (Exception e) {
            throw new RuntimeException("Could not receive message from SQS queue: " + e.getMessage());
        }
    }

    private void deleteMessage(SqsClient client, String messageReceipt) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(messageReceipt)
                .build();

        try {
            client.deleteMessage(request);
        } catch (Exception e) {
            log(LogLevel.WARN, "Failed to delete SQS message with receipt: %s", messageReceipt);
        }
    }
}
