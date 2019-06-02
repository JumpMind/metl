// <Imports>
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
// end

// onHandleMessage(inputMessage, messageTarget) 
if (!unitOfWorkBoundaryReached) {
	// assumes the input message is an xml string that is to be saved to a file and uploaded
    String xml = inputMessage.getPayload().get(0);
    // file_dir would be the directory (path) (set in flow settings) and xmlFileName (generated as a header parameter in separate script)
    // is the name to where the file will be created before sending
    String fileName = context.flowParameters['file_dir'] + '/' + inputMessage.getHeader().get("xmlFileName")+ '.xml';
    info("Filename is " + fileName);
    
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost(resource.getResourceRuntimeSettings().get("url"));  // gets the url value from the Http resource 
    MultipartEntity entity = new MultipartEntity();
    entity.addPart("Filedata", new ByteArrayBody(xml.getBytes("UTF-8"), fileName));
    httpPost.setEntity(entity);
    httpClient.execute(httpPost);
    
    callback.sendControlMessage();
}
// end

