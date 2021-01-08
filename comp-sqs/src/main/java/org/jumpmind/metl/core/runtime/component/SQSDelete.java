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
import org.jumpmind.metl.core.runtime.MessageHeader;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

public class SQSDelete extends AbstractComponentRuntime {

    public final static String TYPE = "SQS Delete";
    public final static String MESSAGE_HEADER_KEY = "sqsMessageReceipt";

    public final static String SQS_DELETE_QUEUE_URL = "sqs.delete.queue.url";

    /* settings */
    String queueUrl;

    @Override
    public void start() {
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("SQS queue resource must be defined");
        }

        TypedProperties properties = getTypedProperties();
        queueUrl = properties.get(SQS_DELETE_QUEUE_URL);
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (!(inputMessage instanceof ControlMessage)) {
            try {
                SqsClient client = (SqsClient)getResourceReference();

                MessageHeader receiptHandle = inputMessage.getHeader();
                String receiptString = receiptHandle.get(MESSAGE_HEADER_KEY).toString();

                if (receiptString != null && receiptString != "") {
                    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(receiptString)
                            .build();

                    client.deleteMessage(deleteMessageRequest);
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not delete message from SQS queue: " + e);
            }
        }
    }
}
