// <Methods>
int recordCount;
// end

// onHandleMessage(inputMessage, messageTarget) 
if (inputMessage instanceof ContentMessage) {
    recordCount += inputMessage.getPayload().size();
}
// end

// onSuccess()
// do something with results like update a job status table
// end