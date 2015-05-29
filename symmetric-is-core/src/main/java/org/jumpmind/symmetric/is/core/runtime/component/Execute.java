package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.util.FormatUtils;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = Execute.TYPE,
        inputMessage = MessageType.ANY,
        outgoingMessage = MessageType.TEXT,
        iconImage = "execute.png")
public class Execute extends AbstractComponentRuntime {

    public static final String TYPE = "Execute";

    @SettingDefinition(order = 1, required = true, type = Type.TEXT, defaultValue = "", label = "Command")
    public final static String COMMAND = "command";

    @SettingDefinition(order = 10, type = Type.BOOLEAN, defaultValue = "false", label = "Continue On Error")
    public final static String CONTINUE_ON_ERROR = "continue.on.error";

    @SettingDefinition(order = 20, type = Type.INTEGER, defaultValue = "0", label = "Success Code")
    public final static String SUCCESS_CODE = "success.code";

    String[] commands;

    boolean continueOnError = false;

    int successCode = 0;

    @Override
    protected void start() {
        String line = FormatUtils.replaceTokens(getComponent().get(COMMAND, null), context.getFlowParametersAsString(), true);
        continueOnError = getComponent().getBoolean(CONTINUE_ON_ERROR, continueOnError);
        successCode = getComponent().getInt(SUCCESS_CODE, successCode);

        if (isBlank(line)) {
            throw new IllegalStateException("A command is required by this component");
        }
        try {
            CsvReader csvReader = new CsvReader(new ByteArrayInputStream(line.getBytes()), Charset.forName("utf-8"));
            csvReader.setDelimiter(' ');
            csvReader.setTextQualifier('"');
            csvReader.setUseTextQualifier(true);
            csvReader.setEscapeMode(CsvReader.ESCAPE_MODE_BACKSLASH);
            if (csvReader.readRecord()) {
                commands = csvReader.getValues();
            }
        } catch (Exception e) {
            throw new IoException(e);
        }

    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PumpStreamHandler outputHandler = new PumpStreamHandler(os);
            org.apache.tools.ant.taskdefs.Execute antTask = new org.apache.tools.ant.taskdefs.Execute(outputHandler);
            antTask.setCommandline(commands);
            info("About to execute: %s", ArrayUtils.toString(commands));
            int code = antTask.execute();
            String output = new String(os.toByteArray());
            if (successCode == code || continueOnError) {
                if (successCode == code) {
                    info("Returned an code of %d", code);
                } else {
                    warn("Returned an code of %d", code);
                }
                info("The output of the command was: %s", output);

                getComponentStatistics().incrementOutboundMessages();
                Message msg = inputMessage.copy(getFlowStepId());
                ArrayList<String> payload = new ArrayList<String>();
                payload.add(output);
                msg.setPayload(payload);
                messageTarget.put(msg);
            } else {
                info("The output of the command was: %s", output);
                throw new IoException("%s failed with an error code of %d", ArrayUtils.toString(commands), code);
            }
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

}
