package org.jumpmind.symmetric.is.core.config.data;

public class ComponentVersionData extends AbstractVersionData {

    private static final long serialVersionUID = 1L;

    String inputModelVersiondId;
    String outputModelVersionId;
    String connectionId;

    public String getInputModelVersiondId() {
        return inputModelVersiondId;
    }

    public void setInputModelVersiondId(String inputModelVersiondId) {
        this.inputModelVersiondId = inputModelVersiondId;
    }

    public String getOutputModelVersionId() {
        return outputModelVersionId;
    }

    public void setOutputModelVersionId(String outputModelVersionId) {
        this.outputModelVersionId = outputModelVersionId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

}
