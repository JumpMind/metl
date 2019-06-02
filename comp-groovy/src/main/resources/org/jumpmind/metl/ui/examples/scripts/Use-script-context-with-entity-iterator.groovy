// <Imports>
import java.util.List;
import org.jumpmind.metl.core.model.*;

// end

// onInit()
String combinedList = '';
int msgCount = 0;
scriptContext.put("combinedList", combinedList);
scriptContext.put("msgCount", msgCount);

// end

// onHandleMessage(inputMessage, messageTarget) 
// Get the values stored in the scriptContext from prior messages processed
String allCombinedList = scriptContext.get("combinedList");
int localMsgCount = scriptContext.get("msgCount");

if (!(inputMessage instanceof ControlMessage)) {
    localMsgCount++;
    Model inputModel = flowStep.getComponent().getInputModel();

	// make sure we have a valid inputModel and then get the model Entity and ids of the Attributes for that Entity
    if (inputModel != null) {
        ModelEntity myEntity = inputModel.getEntityByName("Entity_Name");
        ModelAttrib myAttr1Attribute = testEntity.getModelAttributeByName("Attribute1_Name");
        ModelAttrib myAttr2Attribute = testEntity.getModelAttributeByName("Attribute2_Name");

        List<EntityData> list = inputMessage.getPayload();
        entityDataIterator = list.iterator();

		// walk through all the entity data objects getting the attributes from each row using the attribute id found above
        while (entityDataIterator.hasNext()) {  
            EntityData data = entityDataIterator.next();

            String attr1 = (String) data.get(myAttr1Attribute.getId());
            String attr2 = (String) data.get(myAttr2Attribute.getId());

			// build our combined list of attr2 data values
            allCombinedList += '<some_tag>' + attr1 + ':' + attr2 + '</some_tag>';   
        }
    }
    
    info("Combined List msg(" + localMsgCount + ") : " + allCombinedList);
    // put the updated values back in the scriptContext for reference with next message(s)
    scriptContext.put("combinedList", allCombinedList);
    scriptContext.put("msgCount", localMsgCount);
}

// Only forward one message when the unit of work boundary was reached with the combined set of values 
if (unitOfWorkBoundaryReached) {
    info("Processed " + localMsgCount + " messages");
    info("Combined data: " + allCombinedList);

    params = [combined_list:allCombinedList];
    forwardMessageWithParameters(params);
}

// end
