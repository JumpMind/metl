package org.jumpmind.symmetric.is.core.model;


public class ModelAttribute extends AbstractObject {

    private static final long serialVersionUID = 1L;

    ModelEntity entity;
    
    ModelEntity typeEntity;    

    String name;

    DataType type;

    String typeEntityId;

    public ModelAttribute() {
    	
    }
    
    public ModelAttribute(String id, String entityId, String name) {
        this.id = id;
        setEntityId(entityId);
        this.name = name;
    }
    
    public ModelAttribute(String id, ModelEntity entity, String name) {
        this.id = id;
        this.entity = entity;
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
		return entity != null ? entity.getId() : null;
	}

	public void setEntityId(String entityId) {
		if (entityId != null) {
		    entity = new ModelEntity(entityId, entityId);
		} else {
		    entityId = null;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DataType getDataType() {
		return type;
	}

	public String getType() {
		return type.toString();
	}

	public void setType(String type) {
		this.type = DataType.valueOf(type);
	}

	public void setDataType(DataType type) {
		this.type = type;
	}

	public String getTypeEntityId() {
		return typeEntityId;
	}

	public void setTypeEntityId(String typeEntityId) {
		this.typeEntityId = typeEntityId;
	}

	public ModelEntity getEntity() {
		return entity;
	}

	public void setEntity(ModelEntity entity) {
		this.entity = entity;
	}

}
