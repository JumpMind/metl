package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = ScriptExecutor.TYPE,
        inputMessage = MessageType.ANY,
        outgoingMessage = MessageType.ANY,
        iconImage = "script.png")
public class ScriptExecutor extends AbstractComponent {

    public static final String TYPE = "Script";

    @SettingDefinition(order = 0, required = true, type = Type.TEXT, label = "Script")
    public final static String SCRIPT = "script";

    public static String TRANSFORM_EXPRESSION = "transform.expression";

    String script;

    @Override
    public void start(IExecutionTracker executionTracker) {
        super.start(executionTracker);

        script = flowStep.getComponent().get(SCRIPT);

    }

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        Message outputMessage = MessageScriptHelper.eval(flow, flowStep, componentStatistics, inputMessage, script);
        if (outputMessage != null) {
            componentStatistics.incrementOutboundMessages();
            messageTarget.put(outputMessage);
        }
    }

}
