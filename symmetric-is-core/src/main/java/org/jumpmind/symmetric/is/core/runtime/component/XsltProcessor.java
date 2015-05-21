package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = XsltProcessor.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "xsltprocessor.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.TEXT)
public class XsltProcessor extends AbstractComponentRuntime {

    public static final String TYPE = "XSLT Processor";

    @Override
    protected void start() {
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
    }

}
