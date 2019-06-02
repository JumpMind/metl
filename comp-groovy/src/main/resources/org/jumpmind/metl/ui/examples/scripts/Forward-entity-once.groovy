// onHandleMessage(inputMessage, messageTarget)
if (inputMessage instanceof EntityDataMessage) {
    def datas = inputMessage.getPayload()
    for (EntityData data: datas) {
        def id = new Integer(getAttributeValue("SOME_ENTITY", "SOME_ATTRIBUTE", data))
        def alreadySent = scriptContext.get(id)
        if (alreadySent == null) {
            scriptContext.put(id, Boolean.TRUE)
            callback.sendEntityDataMessage(null, [data]);
        }
    }
}
// end