package org.jumpmind.symmetric.is.core.model;

import java.util.ArrayList;
import java.util.List;

public class Project extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String name;

    String description;

    List<ProjectVersion> projectVersions;

    boolean deleted;

    public Project() {
        this.projectVersions = new ArrayList<ProjectVersion>();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public List<ProjectVersion> getProjectVersions() {
        return projectVersions;
    }

    public void setProjectVersions(List<ProjectVersion> projectVersions) {
        this.projectVersions = projectVersions;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }
    
    public ProjectVersion getLatestProjectVersion() {
        ProjectVersion version = null;
        for (ProjectVersion projectVersion : projectVersions) {
            if (version == null || version.getCreateTime().before(projectVersion.getCreateTime())) {
                version = projectVersion;
            }
        }
        return version;
    }
}
