package org.jumpmind.symmetric.is.core.config.data;

public class ModelEntityRelationshipData extends AbstractData {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    String sourceEntityId;
    
    String targetEntityId;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSourceEntityId() {
		return sourceEntityId;
	}

	public void setSourceEntityId(String sourceEntityId) {
		this.sourceEntityId = sourceEntityId;
	}

	public String getTargetEntityId() {
		return targetEntityId;
	}

	public void setTargetEntityId(String targetEntityId) {
		this.targetEntityId = targetEntityId;
	}

}
