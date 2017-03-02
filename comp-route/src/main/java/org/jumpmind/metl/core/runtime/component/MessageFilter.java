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

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class MessageFilter extends AbstractComponentRuntime {

    public final static String SETTING_FILTER_EXPRESSION = "filter.expression";

    public final static String SETTING_MESSAGE_TYPE_TO_FILTER = "message.type.to.filter";

    public final static String MESSAGE_TYPE_TO_FILTER_CONTENT = "CONTENT";

    public final static String MESSAGE_TYPE_TO_FILTER_CONTROL = "CONTROL";

    ScriptEngine scriptEngine;

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (scriptEngine == null) {
            scriptEngine = new GroovyScriptEngineImpl();
        }

        String messageTypeToFilter = properties.get(SETTING_MESSAGE_TYPE_TO_FILTER);
        String expression = properties.get(SETTING_FILTER_EXPRESSION);
        Bindings bindings = scriptEngine.createBindings();
        bindHeadersAndFlowParameters(bindings, inputMessage);
        try {
            if (MESSAGE_TYPE_TO_FILTER_CONTENT.equals(messageTypeToFilter) && inputMessage instanceof ContentMessage) {
                if (Boolean.TRUE.equals(scriptEngine.eval(expression, bindings))) {
                    callback.forward(inputMessage);
                }
            } else if (MESSAGE_TYPE_TO_FILTER_CONTROL.equals(messageTypeToFilter) && inputMessage instanceof ControlMessage) {
                if (Boolean.TRUE.equals(scriptEngine.eval(expression, bindings))) {
                    callback.sendControlMessage();
                }
            } else if (inputMessage instanceof ControlMessage) {
                callback.sendControlMessage();
            }
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

    }

}
