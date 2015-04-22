package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.sql.Row;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CharacterDataArea;

public class MessageScriptHelper {

	protected IComponent component;

    protected Iterator<EntityData> entityDataIterator;

    protected Message inputMessage;

    protected IMessageTarget messageTarget;

    public MessageScriptHelper(IComponent component) {
        this.component = component;
    }

    protected JdbcTemplate getJdbcTemplate() {
        DataSource ds = component.getResource().reference();
        return new JdbcTemplate(ds);
    }

    protected BasicDataSource getBasicDataSource() {
        return (BasicDataSource) component.getResource().reference();
    }

    protected Row nextRowFromInputMessage() {
        if (component.getFlowStep().getComponent().getInputModel() != null) {
            if (entityDataIterator == null) {
                List<EntityData> list = inputMessage.getPayload();
                entityDataIterator = list.iterator();
            }

            if (entityDataIterator.hasNext()) {
                EntityData data = entityDataIterator.next();
                Model model = component.getFlowStep().getComponent().getInputModel();
                Row row = new Row(data.size());

                Set<String> attributeIds = data.keySet();
                for (String attributeId : attributeIds) {
                    ModelAttribute attribute = model.getAttributeById(attributeId);
                    if (attribute != null) {
                        row.put(attribute.getName(), data.get(attributeId));
                    }
                }
                return row;
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException(
                    "The input model needs to be set if you are going to use the entity data");
        }
    }

    protected void info(String message, Object... args) {
        component.getExecutionTracker().log(component.getExecutionId(), LogLevel.INFO, component,
                String.format(message, args));
    }

    protected void setInputMessage(Message inputMessage) {
        this.inputMessage = inputMessage;
    }

    protected void setMessageTarget(IMessageTarget messageTarget) {
        this.messageTarget = messageTarget;
    }

    protected void onInit() {

    }

    protected void onHandle() {
        try {
Row config = nextRowFromInputMessage();
String dsLib = config.getString("DSLIB");
String dbUser = config.getString("user");
String dbPassword = config.getString("secret");
String url = getBasicDataSource().getUrl();
url = url.substring("jdbc:as400://".length());
int nextSlash = url.indexOf("/");
if (nextSlash >= 0) {
	url = url.substring(0, nextSlash);
}
AS400 as400 = new AS400(url, dbUser, dbPassword);
CharacterDataArea updateArea = new CharacterDataArea();
updateArea.setSystem(as400);
updateArea.setPath("/QSYS.LIB/" + dsLib + ".LIB/UO360NRUN.DTAARA");
String status = updateArea.read();
if (status != null) {
    status = status.trim();
}
info("The status file currently reads %s", status);
if ("Data Ready".equals(status)) {
    messageTarget.put(inputMessage);
} else {
    messageTarget.put(new ShutdownMessage(component.getFlowStep().getId(), true));
}
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void onComplete(Throwable myError, List<Throwable> allErrors) {
    }

}
