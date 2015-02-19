package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class Model extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    Folder folder;
    
    List<ModelVersion> modelVersions;
    
    String name; 
    
    String type;
    
    String folderId;
    
    boolean shared;

    public Model() {
    	modelVersions = new ArrayList<ModelVersion>();
    }
    
    public Model(Folder folder) {
    	this();
    	this.folder = folder;
    }
    
	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String folderId) {
		this.folderId = folderId;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public List<ModelVersion> getModelVersions() {
		return modelVersions;
	}

	public void setModelVersions(List<ModelVersion> modelVersions) {
		this.modelVersions = modelVersions;
	}
    
    
}
