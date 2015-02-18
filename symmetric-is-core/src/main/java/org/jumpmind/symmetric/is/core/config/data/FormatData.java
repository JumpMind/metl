package org.jumpmind.symmetric.is.core.config.data;

public class FormatData extends AbstractData {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    String type;
    
    String folderId;
    
    boolean shared;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

}
