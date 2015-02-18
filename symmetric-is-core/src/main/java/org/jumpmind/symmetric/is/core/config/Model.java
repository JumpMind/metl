package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelData;

public class Model extends AbstractObject<ModelData> {

    private static final long serialVersionUID = 1L;
    
    Folder folder;
    
    List<ModelVersion> modelVersions;
    
    public Model() {
        super(new ModelData());
    }

    public Model(ModelData data) {
        super(data);
    }
    
    public Model(Folder folder, ModelData data) {
        super(data);
        setFolder(folder);
    }
    
    public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
        if (folder != null) {
            data.setFolderId(folder.getData().getId());
        }
	}

    public void setName(String name) {
        this.data.setName(name);
    }
    
    public String getName() {
        return this.data.getName();
    }
    
    public String getType() {
        return data.getType();
    }

    public boolean isShared() {
        return data.isShared();
    }
    
    public List<ModelVersion> getModelVersions() {
    	if (modelVersions == null) {
    		modelVersions = new ArrayList<ModelVersion>();
    	}
        return modelVersions;
    }
    
}
