import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import model.Trade;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map.Entry;
import java.util.Properties;

import static com.hazelcast.jet.Util.entry;

public class IngestTrades {

    public static final String TOPIC = "trades";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("IngestTrades <bootstrap servers>");
            System.exit(1);
        }
        String servers = args[0];
        JetInstance jet = Jet.newJetClient();

        try {
            JobConfig ingestTradesConfig = new JobConfig()
                    .setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE)
                    .setName("ingestTrades");

            jet.newJobIfAbsent(ingestTrades(servers), ingestTradesConfig).join();
        } finally {
            Jet.shutdownAll();
        }
    }

    private static Pipeline ingestTrades(String servers) {
        Pipeline p = Pipeline.create();

        p.drawFrom(KafkaSources.<String, String, Entry<String, Trade>>kafka(kafkaSourceProps(servers),
                record -> {
                    Trade trade = new Trade(record.value());
                    return entry(trade.getTradeId(), trade);
                }, TOPIC))
         .withoutTimestamps()
         .setLocalParallelism(2)
         .drainTo(Sinks.map("trades"));

        return p;
    }

    private static Properties kafkaSourceProps(String servers) {
        Properties props = new Properties();
        props.setProperty("auto.offset.reset", "earliest");
        props.setProperty("bootstrap.servers", servers);
        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", StringDeserializer.class.getName());
        return props;
    }

}
