package org.jumpmind.symmetric.is.core.model;

public class ProjectVersion extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Project project;
    
    String description;

    String origProjectId;
    
    String versionLabel;

    boolean locked;

    boolean archived;
    
    boolean deleted;
    
    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
    
    public String getVersionLabel() {
        return versionLabel;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getName() {
        return project.getName();
    }

    public void setProjectId(String projectId) {
        if (projectId != null) {
            project = new Project();
            project.setId(projectId);
        } else {
            project = null;
        }
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    public Project getProject() {
        return project;
    }

    public String getProjectId() {
        return project != null ? project.getId() : null;
    }

    public void setOrigProjectId(String origProjectId) {
        this.origProjectId = origProjectId;
    }

    public String getOrigProjectId() {
        return origProjectId;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }   

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
