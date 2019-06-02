// onHandleMessage(inputMessage, messageTarget)
// Create a fileName parameter that appends the date time that can be used in subsequent steps such as a Text File Writer 
Date date = new Date();
String dateTime = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(date);

fileName = 'MyFile_' + dateTime + '.txt';
forwardMessageWithParameters(['filename.property':fileName]);
// end