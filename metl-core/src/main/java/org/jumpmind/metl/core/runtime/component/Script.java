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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityData.ChangeType;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Script extends AbstractComponentRuntime {

    public static final String TYPE = "Script";

    public final static String IMPORTS = "imports";

    public final static String INIT_SCRIPT = "init.script";

    public final static String HANDLE_SCRIPT = "handle.msg.script";

    public final static String ON_FLOW_SUCCESS = "on.flow.success.script";
    
    public final static String ON_FLOW_ERROR = "on.flow.error.script";

    public static String TRANSFORM_EXPRESSION = "transform.expression";

    ScriptEngine engine;
    
    String runWhen = PER_MESSAGE;

    @Override
    protected void start() {
        runWhen = getComponent().get(RUN_WHEN, PER_MESSAGE);
        String importStatements = getComponent().get(IMPORTS);
        String initScript = getComponent().get(INIT_SCRIPT);
        String handleMessageScript = getComponent().get(HANDLE_SCRIPT);
        String onSuccess = getComponent().get(ON_FLOW_SUCCESS);
        String onError = getComponent().get(ON_FLOW_ERROR);

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("groovy");

        engine.put("component", this);        
        StringBuilder script = new StringBuilder();
        try {
            script.append(String.format("import %s;\n", ISendMessageCallback.class.getName()));
            script.append(String.format("import %s.*;\n", Message.class.getPackage().getName()));
            script.append(String.format("import %s;\n", MessageScriptHelper.class.getName()));
            script.append(String.format("import %s.%s;\n", EntityData.class.getName(), ChangeType.class.getSimpleName()));
            script.append("import org.jumpmind.db.sql.*;\n");
            if (isNotBlank(importStatements)) {
                script.append(importStatements);
            }
            script.append("\n");
            script.append(String.format("helper = new %1$s(component) { \n",
                    MessageScriptHelper.class.getSimpleName()));
            if (isNotBlank(initScript)) {
                script.append("\n");
                script.append(String.format(" protected void onInit() { %s} \n", initScript));
            }
            if (isNotBlank(handleMessageScript)) {
                script.append("\n");
                script.append(String.format(" protected void onHandle() { %s } \n",
                        handleMessageScript));
            }
            if (isNotBlank(onSuccess)) {
                script.append("\n");
                script.append(String
                        .format(" protected void onSuccess() { %s } \n",
                                onSuccess));
            }
            if (isNotBlank(onError)) {
                script.append("\n");
                script.append(String
                        .format(" protected void onError(Throwable myError, List<Throwable> allErrors) { %s } \n",
                                onError));
            }
            script.append("\n};\n");

            log(LogLevel.DEBUG, script.toString());
            script.append("helper.onInit();");
            engine.eval(script.toString());
            this.engine = engine;
        } catch (ScriptException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause != null) {
                if (rootCause instanceof RuntimeException) {
                    throw (RuntimeException) rootCause;
                } else {
                    throw new RuntimeException(rootCause);
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }
    
    @Override
    public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
        boolean runScript = false;
        if (PER_ENTITY.equals(runWhen) 
                && !(inputMessage instanceof ControlMessage)) {
            runScript = true;
        } else if (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage)) {
            runScript = true;            
        } else if (PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage) {
            runScript = true;            
        }

        if (runScript) {
            invoke("setInputMessage", inputMessage);
            invoke("setSendMessageCallback", messageTarget);
            invoke("setUnitOfWorkBoundaryReached", unitOfWorkBoundaryReached);
            invoke("onHandle");
        }
    }

    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        invoke("onError", myError);
    }

    @Override
    public void flowCompleted(boolean cancelled) {
        invoke("onSuccess");
    }

    protected void invoke(String method, Object... args) {
        if (engine != null) {
            try {
                Invocable invocable = (Invocable) engine;
                Object helper = engine.get("helper");
                invocable.invokeMethod(helper, method, args);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                if (rootCause != null) {
                    if (rootCause instanceof RuntimeException) {
                        throw (RuntimeException) rootCause;
                    } else {
                        throw new RuntimeException(rootCause);
                    }
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
