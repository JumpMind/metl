package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.jumpmind.metl.core.model.EntityRow;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.springframework.util.MimeTypeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class HttpRequest extends AbstractComponentRuntime {

    public static final String REQUEST_PAYLOAD = HttpRequest.class.getName() + ".REQUEST_PAYLOAD";

    public static final String CONTENT_TYPE = HttpRequest.class.getName() + ".CONTENT_TYPE";

    public static final String PATH = "path";

    public static final String HTTP_METHOD = "http.method";
    
    public static final String PAYLOAD_FORMAT = "payload.format";

    public HttpRequest() {
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        String requestPayload = getComponentContext().getFlowParameters().get(REQUEST_PAYLOAD);

        if (isNotBlank(requestPayload)) {
            Model outputModel = getOutputModel();
            if (outputModel != null) {
                try {
                    String contentType = context.getFlowParameters().get(HttpHeaders.CONTENT_TYPE);
                    List<EntityRow> entityRows = null;
                    ObjectMapper mapper = null;
                    if (MimeTypeUtils.APPLICATION_XML.toString().equals(contentType)) {
                        mapper = new XmlMapper();
                    } else {
                        /* default to parse json */
                        mapper = new ObjectMapper();
                    }

                    entityRows = mapper.readValue(requestPayload, mapper.getTypeFactory()
                            .constructCollectionType(List.class, EntityRow.class));

                    ArrayList<EntityData> payload = new ArrayList<>();
                    for (EntityRow entityRow : entityRows) {
                        EntityData data = entityRow.toEntityData(outputModel);
                        if (data != null) {
                            payload.add(data);
                        }
                    }
                    callback.sendEntityDataMessage(inputMessage.getHeader(), payload);

                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } else {
                ArrayList<String> payload = new ArrayList<>(1);
                payload.add(requestPayload);
                callback.sendTextMessage(inputMessage.getHeader(), payload);
            }
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    protected void start() {
    }

}
