import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.config.JetConfig;

public class Server {

    public static void main(String[] args) {
        JetConfig jetConfig = new JetConfig();
        jetConfig.getHazelcastConfig()
                 .addMapConfig(new MapConfig("query1_Trades")
                         .setInMemoryFormat(InMemoryFormat.BINARY));
        Jet.newJetInstance(jetConfig);
    }
}
