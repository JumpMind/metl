package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.FormatData;

public class Format extends DeprecatedAbstractObject<FormatData> {

    private static final long serialVersionUID = 1L;

    Folder folder;
    
    List<FormatVersion> formatVersions;
    
    public Format() {
    	super(new FormatData());
    }
    
    public Format(FormatData data) {
        super(data);
    }

    public Format(Folder folder, FormatData data) {
    	super(data);
    	this.folder = folder;
    }

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public List<FormatVersion> getFormatVersions() {
		if (formatVersions == null) {
			formatVersions = new ArrayList<FormatVersion>();
		}
		return formatVersions;
	}
	
	public String getName() {
		return this.getData().getName();
	}
	
	public void setName(String name) {
		this.getData().setName(name);
	}
	
	public String getType() {
		return this.getData().getType();
	}
	
	
}