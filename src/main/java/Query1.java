import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;
import model.Trade;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import static com.hazelcast.jet.aggregate.AggregateOperations.allOf;
import static com.hazelcast.jet.aggregate.AggregateOperations.averagingLong;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.aggregate.AggregateOperations.summingLong;

public class Query1 {

    public static final String TOPIC = "trades";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("IngestTrades <bootstrap servers>");
            System.exit(1);
        }
        String servers = args[0];

        JetInstance jet = Jet.newJetClient();

        try {
            JobConfig query1config = new JobConfig()
                    .setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE)
                    .setName("Query1");

            jet.newJobIfAbsent(query1(servers), query1config);

        } finally {
            Jet.shutdownAll();
        }
    }

    private static Pipeline query1(String servers) {
        Pipeline p = Pipeline.create();

        StreamStage<Trade> source =
                p.drawFrom(KafkaSources.<String, String, Trade>kafka(kafkaSourceProps(servers),
                        record -> new Trade(record.value()), TOPIC))
                 .withoutTimestamps();


        StreamStage<Entry<String, Tuple3<Long, Long, Double>>> aggregated =
                source
                        .groupingKey(Trade::getSymbol)
                        .rollingAggregate(allOf(counting(), summingLong(Trade::getPrice), averagingLong(Trade::getPrice)))
                        .setName("sum by symbol");

        // write results to IMDG IMap
        aggregated
                .drainTo(Sinks.map("query1_Results"));


        // write results to Kafka topic
//        aggregated
//                .drainTo(KafkaSinks.kafka(kafkaSinkProps(servers), "query1_Results"));


        // add detail rows to IMap
        source
                .drainTo(Sinks.<Trade, String, List<String>>mapWithUpdating(
                        "query1_Trades", trade -> trade.getSymbol(),
                        (list, trade) -> {
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.add(trade.getTradeId());
                            return list;
                        }));

        // add detail rows to multi map as (symbol, trade)
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

    private static Properties kafkaSinkProps(String servers) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", servers);
        props.setProperty("key.serializer", StringSerializer.class.getName());
        props.setProperty("value.serializer", LongSerializer.class.getName());
        return props;
    }
}
