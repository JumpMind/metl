// <Imports>
import org.springframework.dao.EmptyResultDataAccessException;
// end

// <Methods>
// Gets the next id from a table where my value matches a lookup value attribute then set this as the value associated with the ID attribute
int addId(String entityName, EntityData data, int id) {
    if (containsEntity(entityName, data)) {
        String lookupValue = (String)getAttributeValue(entityName, "LOOKUP_VALUE", data);
        id = getId(id, lookupValue);
        putAttributeValue(entityName, "ID", data, id);
   }
    return id;
}

int getId(int id, String lookupValue) {
    if (id < 0) {
    	// get the id corresponding to my lookup value or else get an id one larger than the max if the record doesn't exist
        try {
            id = getJdbcTemplate().queryForObject("select max(ID_FIELD) from MY_TABLE where LOOKUP_FIELD = '" + lookupValue + "'", Integer.class);
        } catch (EmptyResultDataAccessException) { 
            id = getJdbcTemplate().queryForObject("select max(ID_FIELD) + 1 from MY_TABLE", Integer.class);    
        }
    }
    return id;
}
// end

// onHandleMessage(inputMessage, messageTarget)
if (isEntityDataMessage()) {
    List<EntityData> datas = inputMessage.getPayload();
    for (EntityData data : datas) {
        int id = -1;
        
        // call method to add the id to the Entity Attribute value
        id = addId("ENTITY_NAME", data, id);
    }
}

forwardMessage()
// end
