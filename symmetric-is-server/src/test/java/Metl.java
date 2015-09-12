import org.jumpmind.symmetric.WebServer;
import org.jumpmind.symmetric.is.ui.init.AppInitializer;


public class SymmetricIS {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        WebServer webserver = new WebServer();
        webserver.setRegisteredAppInitializers(new Class[] {
                AppInitializer.class
        });
        webserver.setWebAppDir("../symmetric-is-ui/src/main/webapp");
        webserver.setHttpPort(42000);
        webserver.setJmxEnabled(false);
        webserver.setJoin(true);
        webserver.start();        
    }

}
