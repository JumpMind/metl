import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StressTest {

    public StressTest() {
    }

    public static void main(String[] args) throws Exception {
        int count = 1;
        while (true) {
            System.out.println("Queueing up " + count + " threads ");
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            StringBuilder result = new StringBuilder();
                            URL url = new URL("http://l02dlascsymds01.corp.local:42000/metl/api/ws/jussit/customer/phone/6146686601");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String line;
                            while ((line = rd.readLine()) != null) {
                                result.append(line);
                            }
                            rd.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                threads.add(thread);
                thread.start();                
            }
            
            for (Thread thread : threads) {                
                while (thread.isAlive()) {
                    try {
                        Thread.sleep(5);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

}
