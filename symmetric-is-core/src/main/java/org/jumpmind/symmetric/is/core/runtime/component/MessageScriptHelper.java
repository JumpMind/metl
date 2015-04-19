package org.jumpmind.symmetric.is.core.runtime.component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.Message;

public class MessageScriptHelper {

    static private ThreadLocal<ScriptEngine> scriptEngine = new ThreadLocal<ScriptEngine>();

    Message inputMessage;

    Flow flow;

    FlowStep flowStep;

    ComponentStatistics componentStatistics;

    public MessageScriptHelper(Flow flow, FlowStep flowStep,
            ComponentStatistics componentStatistics, Message inputMessage) {
        this.flow = flow;
        this.flowStep = flowStep;
        this.inputMessage = inputMessage;
        this.componentStatistics = componentStatistics;
    }

    public Message eval() {
        return inputMessage;
    }

    public static Message eval(Flow flow, FlowStep flowStep,
            ComponentStatistics componentStatistics, Message inputMessage, String expression) {
        ScriptEngine engine = scriptEngine.get();
        if (engine == null) {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("groovy");
            scriptEngine.set(engine);
        }

        engine.put("flow", flow);
        engine.put("flowStep", flowStep);
        engine.put("inputMessage", inputMessage);
        engine.put("componentStatistics", componentStatistics);

        try {
            StringBuilder script = new StringBuilder();
            script.append(String.format("import %s;\n", MessageScriptHelper.class.getName()));
            script.append(String.format("import %s;\n", Message.class.getName()));
            script.append(String
                    .format("return new %s(flow, flowStep, componentStatistics, inputMessage) { public Message eval() { return %s } }.eval()",
                            MessageScriptHelper.class.getSimpleName(), expression));
            return (Message) engine.eval(script.toString());
        } catch (ScriptException e) {
            throw new RuntimeException("Unable to evaluate groovy script", e);
        }

    }
}
