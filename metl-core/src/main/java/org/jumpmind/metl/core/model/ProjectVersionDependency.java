package org.jumpmind.metl.core.model;

public class ProjectVersionDependency extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String sourceProjectVersionId;
    
    String targetProjectVersionId;    

    /**
     * @return the sourceProjectVersionId
     */
    public String getSourceProjectVersionId() {
        return sourceProjectVersionId;
    }

    /**
     * @param sourceProjectVersionId the sourceProjectVersionId to set
     */
    public void setSourceProjectVersionId(String sourceProjectVersionId) {
        this.sourceProjectVersionId = sourceProjectVersionId;
    }

    /**
     * @return the targetProjectVersionId
     */
    public String getTargetProjectVersionId() {
        return targetProjectVersionId;
    }

    /**
     * @param targetProjectVersionId the targetProjectVersionId to set
     */
    public void setTargetProjectVersionId(String targetProjectVersionId) {
        this.targetProjectVersionId = targetProjectVersionId;
    }
    
    

}
