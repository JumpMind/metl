import org.jumpmind.metl.WebServer;
import org.jumpmind.metl.ui.init.AppInitializer;


public class Metl {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        WebServer webserver = new WebServer();
        webserver.setRegisteredAppInitializers(new Class[] {
                AppInitializer.class
        });
        webserver.setWebAppDir("../metl-ui/src/main/webapp");
        webserver.setHttpPort(42000);
        webserver.setJmxEnabled(false);
        webserver.setJoin(true);
        webserver.start();        
    }

}
