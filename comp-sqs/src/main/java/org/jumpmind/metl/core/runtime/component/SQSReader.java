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

import org.jumpmind.metl.core.runtime.ControlMessage;
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

    public final static String SQS_READER_QUEUE_URL = "sqs.reader.queue.url";
    public final static String SQS_READER_MAX_MESSAGES_READ_AT_ONCE = "sqs.reader.max.messages.read.at.once";
    public final static String SQS_READER_QUEUE_MESSAGES_PER_OUTPUT_MESSAGE = "sqs.reader.queue.messages.per.output.message";
    public final static String SQS_READER_DELETE_AFTER_READ = "sqs.reader.delete.after.read";
    public final static String SQS_READER_READ_UNTIL_QUEUE_EMPTY = "sqs.reader.read.until.queue.empty";
    
    /* settings */
    String runWhen;
    String queueUrl;
    int maxMsgsToReadAtOnce;
    int messagesPerOutputMessage;
    boolean deleteAfterRead;
    boolean readUntilQueueEmpty;

    @Override
    public void start() {
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("SQS queue resource must be defined");
        }

        TypedProperties properties = getTypedProperties();
        runWhen = properties.get(RUN_WHEN);
        queueUrl = properties.get(SQS_READER_QUEUE_URL);
        maxMsgsToReadAtOnce = properties.getInt(SQS_READER_MAX_MESSAGES_READ_AT_ONCE);
        messagesPerOutputMessage = properties.getInt(SQS_READER_QUEUE_MESSAGES_PER_OUTPUT_MESSAGE);
        deleteAfterRead = Boolean.valueOf(properties.getProperty(SQS_READER_DELETE_AFTER_READ));
        readUntilQueueEmpty = Boolean.valueOf(properties.getProperty(SQS_READER_READ_UNTIL_QUEUE_EMPTY));

        if (maxMsgsToReadAtOnce < 1 || maxMsgsToReadAtOnce  > 10) {
            throw new MisconfiguredException("\"Max Messages to Read at Once\" must be between 1 and 10");
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
            ArrayList<String> outputMessages = new ArrayList<>();
            ArrayList<String> messagesRead = readMessages(client);
            outputMessages.addAll(messagesRead);

            while (readUntilQueueEmpty && messagesRead.size() >= maxMsgsToReadAtOnce) {
                messagesRead = readMessages(client);
                outputMessages.addAll(messagesRead);
            }
            callback.sendTextMessage(null, consolidateMessages(outputMessages));
        }
    }

    private ArrayList<String> readMessages(SqsClient client) {
        ArrayList<String> messages = new ArrayList<>();
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMsgsToReadAtOnce)
                .build();

        ReceiveMessageResponse response = null;
        try {
            response = client.receiveMessage(request);
        } catch (Exception e) {
            throw new RuntimeException("Could not receive message from SQS queue: " + e.getMessage());
        }

        response.messages().forEach(message -> {
            messages.add(message.body());
            deleteMessage(client, message);
        });

        return messages;
    }

    private void deleteMessage(SqsClient client, software.amazon.awssdk.services.sqs.model.Message message) {
        if (deleteAfterRead) {
            DeleteMessageRequest request = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();

            try {
                client.deleteMessage(request);
            } catch (Exception e) {
                throw new RuntimeException("Could not delete message from SQS queue: " + e.getMessage());
            }

        }
    }
    private ArrayList<String> consolidateMessages(ArrayList<String> messages) {
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < messages.size(); i += messagesPerOutputMessage) {
            String combinedMessages = "";

            for (int j = 0; j < messagesPerOutputMessage; j++) {
                if ((i+j >= 0) && i+j < messages.size()) {
                    combinedMessages += messages.get(i + j);
                }
            }

            result.add(combinedMessages);
        }

        return result;
    }
}
