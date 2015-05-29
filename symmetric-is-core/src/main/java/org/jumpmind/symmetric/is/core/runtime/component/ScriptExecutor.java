package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = ScriptExecutor.TYPE,
        inputMessage = MessageType.ANY,
        outgoingMessage = MessageType.ANY,
        resourceCategory = ResourceCategory.ANY,
        iconImage = "script.png")
public class ScriptExecutor extends AbstractComponentRuntime {

    public static final String TYPE = "Script";

    @SettingDefinition(order = 5, required = true, type = Type.SCRIPT, visible= false,label = "Import Statements")
    public final static String IMPORTS = "imports";

    @SettingDefinition(order = 10, required = true, type = Type.SCRIPT, visible= false,label = "Init Script")
    public final static String INIT_SCRIPT = "init.script";

    @SettingDefinition(order = 15, required = true, type = Type.SCRIPT, visible= false, label = "Handle Msg Script")
    public final static String HANDLE_SCRIPT = "handle.msg.script";

    @SettingDefinition(
            order = 20,
            required = true,
            type = Type.SCRIPT,
                    visible= false,
            label = "On Complete Script")
    public final static String ON_FLOW_SUCCESS = "on.flow.success.script";
    
    @SettingDefinition(
            order = 20,
            required = true,
            type = Type.SCRIPT,
            visible= false,
            label = "On Complete Script")
    public final static String ON_FLOW_ERROR = "on.flow.error.script";


    public static String TRANSFORM_EXPRESSION = "transform.expression";

    ScriptEngine engine;

    @Override
    protected void start() {
        

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
            script.append(String.format("import %s;\n", IMessageTarget.class.getName()));
            script.append(String.format("import %s.*;\n", Message.class.getPackage().getName()));
            script.append(String.format("import %s;\n", MessageScriptHelper.class.getName()));
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
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        invoke("setInputMessage", inputMessage);
        invoke("setMessageTarget", messageTarget);
        invoke("onHandle");
    }

    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        invoke("onError", myError);
    }

    @Override
    public void flowCompleted() {
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
