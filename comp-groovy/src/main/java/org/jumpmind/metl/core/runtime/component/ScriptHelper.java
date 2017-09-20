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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.jumpmind.db.sql.Row;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.ComponentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Helper class for the {@link Script} component. The Script component builds a
 * Groovy implementation of the {@link ScriptHelper#onInit()},
 * {@link ScriptHelper#onHandle()}, {@link ScriptHelper#onSuccess()} and
 * {@link ScriptHelper#onError(Throwable)} methods using the script settings
 * provided by the component user. Groovy scripts have access to the methods and
 * the fields on this class.
 */
public class ScriptHelper {

    /**
     * Use this to log to the Metl log file.
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The context under which the componenot runtime was called.
     */
    protected ComponentContext context;

    /**
     * The configuration of the flow step.
     */
    protected FlowStep flowStep;

    /**
     * The configuration of the entire flow.
     */
    protected Flow flow;

    /**
     * Don't access this directly. Used by {@link #nextRowFromInputMessage()}
     */
    protected Iterator<EntityData> entityDataIterator;

    /**
     * The message that was received. This can be accessed from the
     * {@link #onHandle()} method.
     */
    protected Message inputMessage;

    /**
     * This is a handle to the API that can be used to send outbound
     * {@link Message}s.
     */
    protected ISendMessageCallback callback;

    /**
     * Access to component statistics.
     */
    protected ComponentStatistics componentStatistics;

    /**
     * Access to the resource runtime if it is set.
     */
    protected IResourceRuntime resource;

    /**
     * A context object that can be used by scripts to save objects between
     * calls to {@link #onHandle()}.
     */
    protected Map<String, Object> scriptContext;

    /**
     * An indicator that a {@link ControlMessage} has been received from each
     * source link.
     */
    protected boolean unitOfWorkBoundaryReached;

    protected IComponentRuntime componentRuntime;
    
    private EntityNameLookup entityNameLookup;

    public ScriptHelper() {
    }
    
    protected void init(IComponentRuntime componentRuntime) {
        this.componentRuntime = componentRuntime;
        this.context = componentRuntime.getComponentContext();
        this.resource = context.getResourceRuntime();
        this.componentStatistics = context.getComponentStatistics();
        this.flow = context.getManipulatedFlow();
        this.flowStep = context.getFlowStep();
        this.scriptContext = new HashMap<String, Object>();
    }
    
    /** 
     * @return the name of flow step from which the input message came from
     * 
     */
    protected String getInputMessageSourceName() {
        if (inputMessage != null) {
            return context.getManipulatedFlow().findFlowStepWithId(inputMessage.getHeader().getOriginatingStepId()).getName();
        } else {
            return null;
        }
    }
    
    /**
     * Test whether the inputMessage is an EntityDataMessage
     * 
     * @return true if so
     */
    protected boolean isEntityDataMessage() {
        return inputMessage instanceof EntityDataMessage;
    }

    /**
     * Test whether the inputMessage is an ControlMessage
     * 
     * @return true if so
     */
    protected boolean isControlMessage() {
        return inputMessage instanceof ControlMessage;
    }

    /**
     * Test whether the inputMessage is an TextMessage
     * 
     * @return true if so
     */
    protected boolean isTextMessage() {
        return inputMessage instanceof TextMessage;
    }

    /**
     * If the resource is a {@link DataSource}, then this method returns a
     * Spring JdbcTemplate for use in the script.
     * 
     * @return {@link JdbcTemplate}
     */
    protected JdbcTemplate getJdbcTemplate() {
        if (resource == null) {
            throw new MisconfiguredException("In order to create a jdbc template, a datasource resource must be defined");
        }
        DataSource ds = resource.reference();
        return new JdbcTemplate(ds);
    }
    
    protected boolean doesFileExist(String relativePath) {
        Object reference = resource.reference();
        if (reference instanceof IDirectory) {
            IDirectory directory = (IDirectory)reference;
            FileInfo file = directory.listFile(relativePath, true);
            return file != null;
        } else {
            return false;
        }        
    }
    
    protected int countFiles(String relativePath) {
        Object reference = resource.reference();
        if (reference instanceof IDirectory) {
            IDirectory directory = (IDirectory)reference;
            return directory.listFiles(true, relativePath).size();
        } else {
            return 0;
        }        
    }    

    /**
     * If the resource is a directory, this is the reference to the directory.
     * 
     * @return {@link IDirectory}
     */
    protected IDirectory getDirectory() {
        if (resource == null) {
            throw new MisconfiguredException("In order to access a directory you must configure a directory resource");
        }

        Object directory = resource.reference();
        if (directory instanceof IDirectory) {
            return (IDirectory) directory;
        } else {
            throw new MisconfiguredException("A directory resource is required");
        }
    }

    /**
     * This is mainly to support unit tests or components that need to copy a
     * classpath resource to a directory resource in a script
     * 
     * @param fileName
     *            The file to extract as a file resource
     */
    protected void classpathToDirectory(String fileName) {
        InputStream is = getClass().getResourceAsStream(fileName);
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        OutputStream os = getDirectory().getOutputStream(fileName, false);
        try {
            IOUtils.copy(is, os);
        } catch (IOException e) {
            throw new IoException(e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * If the resource is a {@link DataSource} then return a reference to the
     * {@link DataSource}.
     * 
     * @return {@link DataSource}
     */
    protected BasicDataSource getBasicDataSource() {
        return (BasicDataSource) resource.reference();
    }

    protected Row nextRowFromInputMessage() {
        if (flowStep.getComponent().getInputModel() != null) {
            if (entityDataIterator == null && inputMessage instanceof EntityDataMessage) {
                entityDataIterator = ((EntityDataMessage) inputMessage).getPayload().iterator();
            }

            if (entityDataIterator != null && entityDataIterator.hasNext()) {
                EntityData data = entityDataIterator.next();
                return flowStep.getComponent().toRow(data, false, true);
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException("The input model needs to be set if you are going to use the entity data");
        }
    }

    /**
     * Call this method to record debug level messages in the component log. Use
     * {@link String#format(String, Object...)} syntax if you are using the
     * args.
     * 
     * @param message
     *            The log message in {@link String#format(String, Object...)}
     *            format
     * @param args
     *            Message arguments
     */
    protected void debug(String message, Object... args) {
        context.getExecutionTracker().log(componentRuntime.getThreadNumber(), LogLevel.DEBUG, context, message, args);
    }

    /**
     * Call this method to record info level messages in the component log. Use
     * {@link String#format(String, Object...)} syntax if you are using the
     * args.
     * 
     * @param message
     *            The log message in {@link String#format(String, Object...)}
     *            format
     * @param args
     *            Message arguments
     */
    protected void info(String message, Object... args) {
        context.getExecutionTracker().log(componentRuntime.getThreadNumber(), LogLevel.INFO, context, message, args);
    }

    /**
     * Call this method to record warn level messages in the component log. Use
     * {@link String#format(String, Object...)} syntax if you are using the
     * args.
     * 
     * @param message
     *            The log message in {@link String#format(String, Object...)}
     *            format
     * @param args
     *            Message arguments
     */
    protected void warn(String message, Object... args) {
        context.getExecutionTracker().log(componentRuntime.getThreadNumber(), LogLevel.WARN, context, message, args);
    }

    /**
     * Call this method to record error level messages in the component log. Use
     * {@link String#format(String, Object...)} syntax if you are using the
     * args.
     * 
     * @param message
     *            The log message in {@link String#format(String, Object...)}
     *            format
     * @param args
     *            Message arguments
     */
    protected void error(String message, Object... args) {
        context.getExecutionTracker().log(componentRuntime.getThreadNumber(), LogLevel.ERROR, context, message, args);
    }

    /**
     * Called by the {@link Script} component to set the message prior to
     * calling {@link #onHandle()}
     * 
     * @param inputMessage
     *            Sets the input message
     */
    protected void setInputMessage(Message inputMessage) {
        this.inputMessage = inputMessage;
    }

    /**
     * Called by the {@link Script} component to set the
     * {@link #unitOfWorkBoundaryReached} prior to calling {@link #onHandle()}
     * 
     * @param unitOfWorkBoundaryReached
     *            The unit of work boundary
     */
    protected void setUnitOfWorkBoundaryReached(boolean unitOfWorkBoundaryReached) {
        this.unitOfWorkBoundaryReached = unitOfWorkBoundaryReached;
    }

    /**
     * Called by the {@link Script} component to set the {@link #callback} prior
     * to calling {@link #onHandle()}
     * 
     * @param callback
     *            The callback reference
     */
    protected void setSendMessageCallback(ISendMessageCallback callback) {
        this.callback = callback;
    }

    /**
     * Helper method to check if an entity data contains data for a
     * {@link ModelEntity}
     * 
     * @param entityName
     *            The name to check for
     * @param data
     *            The data object to check
     * @return true when the data object contains data for an
     *         {@link ModelEntity} with a specific name
     */
    protected boolean containsEntity(String entityName, EntityData data) {
        if (entityNameLookup == null) {
            entityNameLookup = new EntityNameLookup(context.getFlowStep().getComponent().getInputModel());
        }
        return entityNameLookup.getEntityNames(data).contains(entityName);
    }

    /**
     * Helper method to set an {@link ModelAttrib} in the {@link EntityData}
     * object.
     * 
     * @param entityName
     *            The name of the {@link ModelEntity}
     * @param attributeName
     *            The name of the {@link ModelAttrib}
     * @param data
     *            The data object on which to set the attribute
     * @param value
     *            The value of the attribute
     */
    protected void putAttributeValue(String entityName, String attributeName, EntityData data, Object value) {
        ModelAttrib attribute = flowStep.getComponent().getInputModel().getAttributeByName(entityName, attributeName);
        data.put(attribute.getId(), value);
    }

    /**
     * Helper method to get an attribute value from the data object by name
     * 
     * @param entityName
     *            The name of the {@link ModelEntity}
     * @param attributeName
     *            The name of the {@link ModelAttrib}
     * @param data
     *            The data object on which to set the attribute
     * @return The value of the attribute
     */
    protected Object getAttributeValue(String entityName, String attributeName, EntityData data) {
        Model model = flowStep.getComponent().getInputModel();
        return ComponentUtils.getAttributeValue(model, data, entityName, attributeName);
    }

    /**
     * Helper method to get an attribute value from the first data object in the
     * current {@link #inputMessage}
     * 
     * @param entityName
     *            The name of the {@link ModelEntity}
     * @param attributeName
     *            The name of the {@link ModelAttrib}
     * @return The value of the attribute
     */
    protected Object getAttributeValue(String entityName, String attributeName) {
        Model model = flowStep.getComponent().getInputModel();
        ArrayList<EntityData> rows = ((EntityDataMessage) inputMessage).getPayload();
        return ComponentUtils.getAttributeValue(model, rows, entityName, attributeName);
    }

    /**
     * Helper method to get an attribute value from the data object by attribute
     * name only.
     * 
     * @param attributeName
     *            The name of the {@link ModelAttrib}
     * @param data
     *            The data object on which to set the attribute
     * @return The value of the attribute
     */
    protected Object getAttributeValue(String attributeName, EntityData data) {
        Model model = flowStep.getComponent().getInputModel();
        return ComponentUtils.getAttributeValue(model, data, attributeName);
    }

    /**
     * Helper method to get a list of attribute values with a specific entity
     * and attribute name from all the data objects in an {@link #inputMessage}
     * 
     * @param entityName
     *            The name of the {@link ModelEntity}
     * @param attributeName
     *            The name of the {@link ModelAttrib}
     * @return A list of attribute values
     */
    protected List<Object> getAttributeValues(String entityName, String attributeName) {
        Model model = flowStep.getComponent().getInputModel();
        ArrayList<EntityData> rows = ((EntityDataMessage) inputMessage).getPayload();
        return ComponentUtils.getAttributeValues(model, rows, entityName, attributeName);
    }
    
    protected void setAttributeValue(String entityName, String attributeName, EntityData data, Object value) {
        Model model = flowStep.getComponent().getInputModel();
        ComponentUtils.setAttributeValue(model, data, entityName, attributeName, value);
    }

    /**
     * Helper method to forward the current {@link #inputMessage}.
     * 
     * @param parameterName
     *            The name of a parameter to add to the message header
     * @param value
     *            The value of the parameter to add to the message header
     */
    protected void forwardMessageWithParameter(String parameterName, Serializable value) {
        Map<String, Serializable> headers = new HashMap<>();
        headers.put(parameterName, value);
        if (inputMessage instanceof ControlMessage) {
            callback.sendControlMessage(headers);
        } else {
            callback.forward(headers, inputMessage);
        }
    }

    /**
     * Helper method to forward the current {@link #inputMessage}.
     * 
     * @param params
     *            Parameters to add to the {@link Message#getHeader()}
     */
    protected void forwardMessageWithParameters(Map<String, Serializable> params) {

        if (inputMessage instanceof ControlMessage) {
            callback.sendControlMessage(params);
        } else {
            callback.forward(params, inputMessage);
        }
    }

    /**
     * Helper method to forward the current {@link #inputMessage}.
     */
    protected void forwardMessage() {
        if (inputMessage instanceof ControlMessage) {
            callback.sendControlMessage(inputMessage.getHeader());
        } else {
            callback.forward(inputMessage);
        }
    }

    /**
     * Helper method to send a control message.
     */
    protected void sendControlMessage() {
        callback.sendControlMessage();
    }        
    
    /**
     * Helper method to send an entity data message.
     */
    protected void sendEntityDataMessage(ArrayList<EntityData> payload) {
        callback.sendEntityDataMessage(null, payload);
    }

    protected void onInit() {
    }

    protected void onHandle() {
        
    }

    protected void onError(Throwable myError) {
    }

    protected void onSuccess() {
    }

}
