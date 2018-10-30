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

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityData.ChangeType;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Script extends AbstractComponentRuntime {

    public static final String TYPE = "Script";

    public final static String IMPORTS = "imports";

    public final static String METHODS = "methods";

    public final static String INIT_SCRIPT = "init.script";

    public final static String HANDLE_SCRIPT = "handle.msg.script";

    public final static String ON_FLOW_SUCCESS = "on.flow.success.script";

    public final static String ON_FLOW_ERROR = "on.flow.error.script";

    public static String TRANSFORM_EXPRESSION = "transform.expression";

    static Map<String, ConcurrentLinkedQueue<ScriptEngine>> enginesByScript = new ConcurrentHashMap<>();

    ScriptEngine engine;
    
    ConcurrentLinkedQueue<ScriptEngine> engineCache;

    @Override
    public void start() {
        long ts = System.currentTimeMillis();
        String importStatements = getComponent().get(IMPORTS);
        String initScript = getComponent().get(INIT_SCRIPT);
        String handleMessageScript = getComponent().get(HANDLE_SCRIPT);
        String methods = getComponent().get(METHODS);
        String onSuccess = getComponent().get(ON_FLOW_SUCCESS);
        String onError = getComponent().get(ON_FLOW_ERROR);

        StringBuilder script = new StringBuilder();
        script.append(String.format("import %s;\n", ISendMessageCallback.class.getName()));
        script.append(String.format("import %s;\n", File.class.getName()));
        script.append(String.format("import %s;\n", FileUtils.class.getName()));
        script.append(String.format("import static %s.*;\n", FileUtils.class.getName()));
        script.append(String.format("import static %s.*;\n", StringUtils.class.getName()));
        script.append(String.format("import %s.*;\n", Message.class.getPackage().getName()));
        script.append(String.format("import %s;\n", ScriptHelper.class.getName()));
        script.append(String.format("import %s;\n", EntityDataMessage.class.getName()));
        script.append(String.format("import %s;\n", EntityData.class.getName()));
        script.append(String.format("import %s;\n", TextMessage.class.getName()));
        script.append(String.format("import %s;\n", ControlMessage.class.getName()));
        script.append(String.format("import %s;\n", BinaryMessage.class.getName()));
        script.append(String.format("import %s;\n", MisconfiguredException.class.getName()));
        script.append(String.format("import %s;\n", AssertException.class.getName()));
        script.append(String.format("import %s.%s;\n", EntityData.class.getName(), ChangeType.class.getSimpleName()));
        script.append("import org.jumpmind.db.sql.*;\n");
        if (isNotBlank(importStatements)) {
            script.append(importStatements);
        }
        script.append("\n");
        script.append(String.format("helper = new %1$s() { \n", ScriptHelper.class.getSimpleName()));

        if (isNotBlank(methods)) {
            script.append("\n");
            script.append(String.format("%s\n", methods));
        }

        if (isNotBlank(initScript)) {
            script.append("\n");
            script.append(String.format(" protected void onInit() { %s \n} \n", initScript));
        }
        if (isNotBlank(handleMessageScript)) {
            script.append("\n");
            script.append(String.format(" protected void onHandle() { %s \n} \n", handleMessageScript));
        }
        if (isNotBlank(onSuccess)) {
            script.append("\n");
            script.append(String.format(" protected void onSuccess() { %s \n} \n", onSuccess));
        }
        if (isNotBlank(onError)) {
            script.append("\n");
            script.append(String.format(" protected void onError(Throwable myError) { %s \n} \n", onError));
        }
        script.append("\n};\n");
        String scriptString = script.toString();
        log(LogLevel.DEBUG, scriptString);
        engineCache = enginesByScript.get(scriptString);
        if (engineCache == null) {
            engineCache = new ConcurrentLinkedQueue<>();
            enginesByScript.put(scriptString, engineCache);
        }

        engine = engineCache.poll();
        if (engine == null) {
            engine = new GroovyScriptEngineImpl();
            try {
                engine.eval(scriptString);
            } catch (ScriptException e) {
                handleScriptException(e);
            }
        }

        try {
            engine.put("component", this);
            engine.eval("helper.init(component)");
            engine.eval("helper.onInit()");
        } catch (ScriptException e) {
            handleScriptException(e);
        }

        log.info("It took {}ms to start the script component", (System.currentTimeMillis() - ts));
    }
    
    private void handleScriptException(ScriptException e) {
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

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
        invoke("setInputMessage", inputMessage);
        invoke("setSendMessageCallback", messageTarget);
        invoke("setUnitOfWorkBoundaryReached", unitOfWorkBoundaryReached);
        invoke("onHandle");
    }

    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        invoke("onError", myError);
        if (engineCache != null) {
            engineCache.offer(engine);
        }
    }

    @Override
    public void flowCompleted(boolean cancelled) {
        invoke("onSuccess");
        if (engineCache != null) {
            engineCache.offer(engine);
        }
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
