package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
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
public class ScriptExecutor extends AbstractComponent {

    public static final String TYPE = "Script";

    @SettingDefinition(order = 10, required = true, type = Type.TEXT, label = "Init Script")
    public final static String INIT_SCRIPT = "init.script";

    @SettingDefinition(order = 15, required = true, type = Type.TEXT, label = "Handle Msg Script")
    public final static String HANDLE_SCRIPT = "handle.msg.script";

    @SettingDefinition(order = 20, required = true, type = Type.TEXT, label = "On Complete Script")
    public final static String ON_FLOW_COMPLETE = "on.flow.complete.script";

    public static String TRANSFORM_EXPRESSION = "transform.expression";

    String initScript;

    String handleMessageScript;

    String onCompleteScript;

    ScriptEngine engine;

    @Override
    public void start(String executionId, IExecutionTracker executionTracker) {
        super.start(executionId, executionTracker);

        initScript = flowStep.getComponent().get(INIT_SCRIPT);
        handleMessageScript = flowStep.getComponent().get(HANDLE_SCRIPT);
        onCompleteScript = flowStep.getComponent().get(ON_FLOW_COMPLETE);

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("groovy");

        engine.put("component", this);

        StringBuilder script = new StringBuilder();
        try {
            script.append(String.format("import %s;\n", IMessageTarget.class.getName()));
            script.append(String.format("import %s;\n", Message.class.getName()));
            script.append(String.format("import %s;\n", MessageScriptHelper.class.getName()));
            script.append(String.format("import %s;\n", Message.class.getName()));
            script.append("import org.jumpmind.db.sql.*;\n");
            script.append(String.format("helper = new %1$s(component) { \n",
                    MessageScriptHelper.class.getSimpleName()));
            if (isNotBlank(initScript)) {
                script.append(String.format(" protected void onInit() { %s} \n", initScript));
            }
            if (isNotBlank(handleMessageScript)) {
                script.append(String.format(" protected void onHandle() { %s } \n",
                        handleMessageScript));
            }
            if (isNotBlank(onCompleteScript)) {
                script.append(String
                        .format(" protected void onComplete(Throwable myError, List<Throwable> allErrors) { %s } \n",
                                onCompleteScript));
            }
            script.append("};\n");
            script.append("helper.onInit();");
            engine.eval(script.toString());
            this.engine = engine;
        } catch (ScriptException e) {
            throw new RuntimeException(String.format("Unable to evaluate groovy script: \n%s",
                    script.toString()), e);
        }

    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        if (engine != null) {
            try {
                Invocable invocable = (Invocable) engine;
                Object helper = engine.get("helper");
                invocable.invokeMethod(helper, "setInputMessage", inputMessage);
                invocable.invokeMethod(helper, "setMessageTarget", messageTarget);
                invocable.invokeMethod(helper, "onHandle");
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Unable to evaluate groovy script", e);
            }
        }
    }

    @Override
    public void flowCompletedWithErrors(Throwable myError, List<Throwable> allErrors) {
        completed(myError, allErrors);
    }

    @Override
    public void flowCompletedWithoutError() {
        List<Throwable> allErrors = Collections.emptyList();
        completed(null, allErrors);
    }

    protected void completed(Throwable myError, List<Throwable> allErrors) {
        if (engine != null) {
            try {
                Invocable invocable = (Invocable) engine;
                Object helper = engine.get("helper");
                invocable.invokeMethod(helper, "onComplete", myError, allErrors);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Unable to evaluate groovy script", e);
            }
        }

    }

}
