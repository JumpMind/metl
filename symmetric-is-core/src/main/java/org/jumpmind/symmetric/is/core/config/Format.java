package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class Format extends AbstractObject {

    private static final long serialVersionUID = 1L;

    Folder folder;
    
    List<FormatVersion> formatVersions;
        
    String name;
    
    String type;
      
    boolean shared;

    public Format() {
    	formatVersions = new ArrayList<FormatVersion>();
    }

    public Format(String id) {
    	this();
    	this.id = id;
    }
    
    public Format(Folder folder) {
    	this();
    	this.folder = folder;
    }
    
	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public List<FormatVersion> getFormatVersions() {
		return formatVersions;
	}

	public void setFormatVersions(List<FormatVersion> formatVersions) {
		this.formatVersions = formatVersions;
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

    public void setFolderId(String folderId) {
        if (folderId != null) {
            this.folder = new Folder(folderId);
        } else {
            this.folder = null;
        }
    }

    public String getFolderId() {
        return folder != null ? folder.getId() : null;
    }

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

}