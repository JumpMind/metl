package org.jumpmind.metl.core.model;

import java.util.UUID;

public class ProjectVersionDepends extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    String projectVersionId;
    
    String targetProjectVersionId;
    
    String rowId;
    
    transient ProjectVersion targetProjectVersion;
    
    @Override
    public void setName(String name) {
    }
    
    @Override
    public String getName() {
        if (targetProjectVersion != null) {
            return String.format("%s (%s)", targetProjectVersion.getProject().getName(),
                    targetProjectVersion.getName());
        } else {
            return targetProjectVersionId;
        }
    }
    
    public void setTargetProjectVersion(ProjectVersion targetProjectVersion) {
        this.targetProjectVersion = targetProjectVersion;
        this.targetProjectVersionId = targetProjectVersion.getId();
    }

    /**
     * @return the sourceProjectVersionId
     */
    public String getProjectVersionId() {
        return projectVersionId;
    }

    /**
     * @param sourceProjectVersionId the sourceProjectVersionId to set
     */
    public void setProjectVersionId(String sourceProjectVersionId) {
        this.projectVersionId = sourceProjectVersionId;
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
    

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }
    
    public String getRowId() {
        if (rowId == null) {
            rowId = UUID.randomUUID().toString();
        }
        return rowId;
    }

}
