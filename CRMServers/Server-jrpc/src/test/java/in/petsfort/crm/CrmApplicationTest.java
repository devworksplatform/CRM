package in.petsfort.crm;

import com.jay.config.JClient;
import com.jay.config.JServer;
import com.jay.data.DataHandler;
import com.jay.http.Request;
import com.jay.http.Response;
import com.jay.transport.TransportProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CrmApplicationTest {
    @TempDir Path temp;

    @Test void studioLifecycleRegistersEveryRpcAndClosesCleanly() {
        System.setProperty("crm.db.path",temp.resolve("lifecycle.db").toString());
        System.setProperty("crm.http.port","0");
        JServer server=new JServer("crm-lifecycle-test",new NoOpTransport());
        CrmApplication application=new CrmApplication();
        try {
            application.init(server);
            assertEquals(1,server.getRpcMappings().size());
            assertEquals(CrmRpc.values().length,server.getRpcMappings().get(0).getRpcs().size());
            assertEquals(64,server.getRpcMappings().get(0).getRpcs().size());
            application.close();
            application.close();
        } finally {
            application.close(); server.close();
            System.clearProperty("crm.db.path"); System.clearProperty("crm.http.port");
        }
    }

    private static final class NoOpTransport implements TransportProvider {
        public void init(Object[] ignored) {}
        public void sendHttpRequest(Request request,DataHandler handler) {}
        public void sendHttpResponse(Response response) {}
        public void sendHttpResponse(String id,JClient client,okhttp3.Response response) {}
        public void listenForDataFromClients(DataHandler handler) {}
        public void sendSshRequest(com.jay.ssh.Request request,DataHandler handler) {}
        public void sendSshResponse(String id,JClient client,String response) {}
        public void sendSshInput(JClient client,String id,String input) {}
    }
}
