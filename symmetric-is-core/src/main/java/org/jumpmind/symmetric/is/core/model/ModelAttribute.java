package org.jumpmind.symmetric.is.core.model;


public class ModelAttribute extends AbstractObject {

    private static final long serialVersionUID = 1L;

    ModelEntity typeEntity;
    
    String entityId;

    String name;

    DataType type;

    String typeEntityId;

    public ModelAttribute() {
    	
    }
    
    public ModelAttribute(String id, String entityId, String name) {
        this.id = id;
        this.entityId = entityId;
        this.name = name;
    }
    
	public ModelEntity getTypeEntity() {
		return typeEntity;
	}

	public void setTypeEntity(ModelEntity typeEntity) {
		this.typeEntity = typeEntity;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DataType getType() {
		return type;
	}

	public void setType(DataType type) {
		this.type = type;
	}

	public String getTypeEntityId() {
		return typeEntityId;
	}

	public void setTypeEntityId(String typeEntityId) {
		this.typeEntityId = typeEntityId;
	}

}
