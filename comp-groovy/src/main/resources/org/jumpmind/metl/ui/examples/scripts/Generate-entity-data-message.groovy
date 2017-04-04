// onHandleMessage(inputMessage, messageTarget)
def list = []
for (int i = 0; i < 10; i++) {
    def data = new EntityData()
    putAttributeValue("NOTE", "ID", data, i)
    putAttributeValue("NOTE", "NOTE", data, "Note #${i}")
    list += data
}
sendEntityDataMessage(list)
// end