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

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class KafkaProducer extends AbstractComponentRuntime {

    public final static String TYPE = "Kafka Producer";

    public final static String KAFKA_PRODUCER_TOPIC_ATTRIBUTE = "kafka.producer.topic.attribute";
    public final static String KAFKA_PRODUCER_TRANSACTION_ID_ATTRIBUTE = "kafka.producer.transaction.id.attribute";
    public final static String KAFKA_PRODUCER_PARTITION_ATTRIBUTE = "kafka.producer.partition.attribute";    
    public final static String KAFKA_PRODUCER_KEY_ATTRIBUTE = "kafka.producer.key.attribute";
    public final static String KAFKA_PRODUCER_VALUE_ATTRIBUTE = "kafka.producer.value.attribute";
    
    /* settings */
    String transactionIdAttribute;
    String partitionAttribute;
    String topicAttribute;
    String keyAttribute;
    String messageAttribute;

    @Override
    public void start() {
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("Kafka publish resource must be defined");
        }
        if (getInputModel() == null) {
            throw new IllegalStateException("Input model must be defined");
        }
        TypedProperties properties = getTypedProperties();
        topicAttribute = properties.get(KAFKA_PRODUCER_TOPIC_ATTRIBUTE);
        transactionIdAttribute = properties.get(KAFKA_PRODUCER_TRANSACTION_ID_ATTRIBUTE);
        partitionAttribute = properties.get(KAFKA_PRODUCER_PARTITION_ATTRIBUTE);
        keyAttribute = properties.get(KAFKA_PRODUCER_KEY_ATTRIBUTE);        
        messageAttribute = properties.get(KAFKA_PRODUCER_VALUE_ATTRIBUTE);
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
        	Producer producer = (Producer)getResourceReference();
        	try {
//	        	producer.beginTransaction();
	            ArrayList<EntityData> inputRows = ((EntityDataMessage) inputMessage).getPayload();
	            for (EntityData inputRow:inputRows) {
	            	String topic = (String) inputRow.get(topicAttribute);
	            	
	            	ProducerRecord rec = new ProducerRecord(topic, 
	            			inputRow.get(keyAttribute), inputRow.get(messageAttribute));
	            	producer.send(rec);
	            }
//	            producer.commitTransaction();
        	} catch (KafkaException ex) {
//        		producer.abortTransaction();
        		throw (RuntimeException) ex;
        	}
        }
    }
}
