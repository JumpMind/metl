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
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

public class SQSReader extends AbstractComponentRuntime {

    public final static String TYPE = "SQS Reader";

    public final static String SQS_READER_QUEUE_URL = "sqs.reader.queue.url";
    public final static String SQS_READER_MAX_MESSAGES_READ_AT_ONCE = "sqs.reader.max.messages.read.at.once";     
    public final static String SQS_READER_QUEUE_MESSAGES_PER_OUTPUT_MESSAGE = "sqs.reader.queue.messages.per.output.message";     
    public final static String SQS_READER_READ_UNTIL_QUEUE_EMPTY = "sqs.reader.read.until.queue.empty";    
    public final static String SQS_READER_RUN_WHEN = "sqs.reader.run.when";    
    
    /* settings */
    String queueUrl;
    String runWhen;
    Integer maxMsgsToReadAtOnce;
    boolean readUntilQueueEmpty;

    @Override
    public void start() {
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("SQS queue resource must be defined");
        }
        TypedProperties properties = getTypedProperties();
        queueUrl = properties.get(SQS_READER_QUEUE_URL);
        maxMsgsToReadAtOnce = properties.getInt(SQS_READER_MAX_MESSAGES_READ_AT_ONCE);
        if (maxMsgsToReadAtOnce > 10) {
        	throw new MisconfiguredException("This must be blah");
        }
        //readUntilQueueEmpty = properties.getInt(SQS_READER_READ_UNTIL_QUEUE_EMPTY);
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
			
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxMsgsToReadAtOnce)
                    .build();
            
        	
        }
    }
}
