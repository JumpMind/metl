package org.jumpmind.symmetric.is.core.config.data;

public class ComponentVersionData extends AbstractVersionData {

    private static final long serialVersionUID = 1L;

    String name;
    String inputModelVersiondId;
    String outputModelVersionId;
    String connectionId;
    String componentId;

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

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
