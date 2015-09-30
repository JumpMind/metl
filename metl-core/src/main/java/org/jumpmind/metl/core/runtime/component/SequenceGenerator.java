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
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.FormatUtils;

public class SequenceGenerator extends AbstractRdbmsComponent {

    public static final String TYPE = "Sequence";

    public final static String SEQ_ATTRIBUTE = "sequence.attribute";

    public final static String SQL = "sequence.sql";
    
    public final static String SHARED = "shared";
    
    public final static String SHARED_NAME = "shared.name";

    String sequenceAttributeId;

    String sql;

    Long nonSharedSequenceNumber;
    
    boolean shared = false;
    
    String sharedName;
    
    static final Map<String, Long> sharedSequence = new HashMap<String, Long>();

    @Override
    protected void start() {
        Component component = getComponent();
        String sequenceAttributeName = component.get(SEQ_ATTRIBUTE);

        shared = getComponent().getBoolean(SHARED, shared);
        if (shared) {
            sharedName = FormatUtils.replaceTokens(getComponent().get(SHARED_NAME), context.getFlowParametersAsString(), true);
            if (sharedName == null) {
                throw new IllegalStateException("The 'Shared Name' must be set when this sequence is shared");
            } else {
                info("This sequence is shared under the name: %s", sharedName);
            }
        }
        
        sql = getComponent().get(SQL);
        if (sql == null) {
            throw new IllegalStateException("An sql statement is required by the " + TYPE);
        }

        Model inputModel = component.getInputModel();
        if (inputModel == null) {
            throw new IllegalStateException("An input model is required by the " + TYPE);
        }

        String[] elements = sequenceAttributeName.split("[.]");
        if (elements.length != 2) {
            throw new IllegalStateException(
                    "The sequence attribute must be specified as 'entity.attribute'");
        }
        sequenceAttributeId = inputModel.getAttributeByName(elements[0], elements[1]).getId();
        if (sequenceAttributeId == null) {
            throw new IllegalStateException(
                    "The sequence attribute must be a valid 'entity.attribute' in the input model.");
        }

        synchronized (SequenceGenerator.class) {
            final String sqlToExecute = FormatUtils.replaceTokens(this.sql, context.getFlowParametersAsString(), true);
            log(LogLevel.DEBUG, "About to run: " + sqlToExecute);
            nonSharedSequenceNumber = getJdbcTemplate().queryForObject(sqlToExecute, context.getFlowParameters(), Long.class);
            if (nonSharedSequenceNumber == null) {
                nonSharedSequenceNumber = 1l;
            }

            if (shared) {
                Long currentValue = sharedSequence.get(sharedName);
                if (currentValue == null || currentValue < nonSharedSequenceNumber) {
                    info("'%s' is setting the shared sequence '%s' to %d", getFlowStep().getName(), sharedName, nonSharedSequenceNumber);
                    sharedSequence.put(sharedName, nonSharedSequenceNumber);
                }
            }
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> outgoingPayload = new ArrayList<EntityData>();
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (EntityData entityData : payload) {
                entityData = entityData.copy();
                long sequence;
                if (shared) {
                    synchronized (SequenceGenerator.class) {
                        Long sequenceNumber = sharedSequence.get(sharedName);
                        sequence = ++sequenceNumber;
                        sharedSequence.put(sharedName, sequenceNumber);                        
                    }
                } else {
                    sequence = ++nonSharedSequenceNumber;
                }
                entityData.put(sequenceAttributeId, sequence);
                getComponentStatistics().incrementNumberEntitiesProcessed();
                outgoingPayload.add(entityData);
            }
            callback.sendMessage(outgoingPayload, unitOfWorkBoundaryReached);
        }
    }

}
