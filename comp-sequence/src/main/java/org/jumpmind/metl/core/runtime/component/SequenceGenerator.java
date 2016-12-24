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
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.FormatUtils;

public class SequenceGenerator extends AbstractRdbmsComponentRuntime {

    public static final String TYPE = "Sequence";

    public final static String SEQ_ATTRIBUTE = "sequence.attribute";

    public final static String SQL = "sequence.sql";
    
    public final static String SHARED = "shared";
    
    public final static String SHARED_NAME = "shared.name";
    
    public final static String RESET_ON_ATTRIBUTE_CHANGE = "reset.on.attribute.change";
    
    public final static String RESET_ATTRIBUTE = "reset.attribute";

    public final static String SEQUENCE_START_VALUE = "sequence.start.value";
    
    String sequenceAttributeId;

    String sql;

    Long nonSharedSequenceNumber;
    
    boolean shared = false;
    
    String sharedName;
    
    boolean resetOnAttributeChange = false;
    
    String resetAttribute;
    
    Long sequenceStartValue;
    
    Object fieldChangeValue = null;
    
    static final Map<String, Long> sharedSequence = new HashMap<String, Long>();

    @Override
    public void start() {
        Component component = getComponent();
        String sequenceAttributeName = component.get(SEQ_ATTRIBUTE);

        shared = getComponent().getBoolean(SHARED, shared);
        if (shared) {
            sharedName = FormatUtils.replaceTokens(getComponent().get(SHARED_NAME), context.getFlowParameters(), true);
            if (sharedName == null) {
                throw new IllegalStateException("The 'Shared Name' must be set when this sequence is shared");
            } else {
                info("This sequence is shared under the name: %s", sharedName);
            }
        }
        
        sql = getComponent().get(SQL);   
        String seqStartString = getComponent().get(SEQUENCE_START_VALUE);
        if (seqStartString != null && !seqStartString.isEmpty()) {
            sequenceStartValue = new Long(seqStartString);
        }
        if ((sql == null || sql.isEmpty()) && sequenceStartValue == null) {
            throw new IllegalStateException("Either a sequence start value or sql statement to get the start value is required");
        }
        
        resetOnAttributeChange = getComponent().getBoolean(RESET_ON_ATTRIBUTE_CHANGE, resetOnAttributeChange);
        if (resetOnAttributeChange) {
            resetAttribute = getComponent().get(RESET_ATTRIBUTE);
            if (resetAttribute == null) {
                throw new IllegalStateException("The 'Reset Attribute' must be set when the sequence is set to reset on attribute change");
            }
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
            
            if (sql != null && !sql.isEmpty()) {            
                final String sqlToExecute = FormatUtils.replaceTokens(this.sql, context.getFlowParameters(), true);
                log(LogLevel.DEBUG, "About to run: " + sqlToExecute);
                nonSharedSequenceNumber = getJdbcTemplate().queryForObject(sqlToExecute, context.getFlowParameters(), Long.class);
                if (nonSharedSequenceNumber == null) {
                    nonSharedSequenceNumber = 1l;
                }
                sequenceStartValue = nonSharedSequenceNumber;
            } else {
                nonSharedSequenceNumber = sequenceStartValue;                
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
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
            ArrayList<EntityData> outgoingPayload = new ArrayList<EntityData>();
            ArrayList<EntityData> payload = ((EntityDataMessage)inputMessage).getPayload();
            for (EntityData entityData : payload) {
                entityData = entityData.copy();
                long sequence;
                if (shared) {
                    synchronized (SequenceGenerator.class) {
                        Long sequenceNumber = sharedSequence.get(sharedName);
                        if (resetNeeded(entityData)) {
                            sequenceNumber = sequenceStartValue;
                        }
                        sequence = ++sequenceNumber;
                        sharedSequence.put(sharedName, sequenceNumber);                        
                    }
                } else {
                    if (resetNeeded(entityData)) {
                        nonSharedSequenceNumber = sequenceStartValue;
                    }
                    sequence = ++nonSharedSequenceNumber;
                }
                entityData.put(sequenceAttributeId, sequence);
                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                outgoingPayload.add(entityData);
            }
            callback.sendEntityDataMessage(null, outgoingPayload);
        }
    }
    
    protected boolean resetNeeded(EntityData entityData) {
        boolean resetNeeded = false;
        if (resetOnAttributeChange && getComponentStatistics().getNumberEntitiesProcessed(getThreadNumber()) != 0) {
            if (!entityData.get(resetAttribute).equals(fieldChangeValue)) {
                resetNeeded = true;
            }
        }
        fieldChangeValue = entityData.get(resetAttribute);
        return resetNeeded;
    }
}
