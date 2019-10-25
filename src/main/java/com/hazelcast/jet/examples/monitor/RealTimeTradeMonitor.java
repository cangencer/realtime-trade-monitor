package com.hazelcast.jet.examples.monitor;/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.examples.monitor.model.Trade;
import com.hazelcast.jet.function.FunctionEx;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamStage;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import static com.hazelcast.jet.Util.entry;
import static com.hazelcast.jet.datamodel.Tuple2.tuple2;

public class RealTimeTradeMonitor {

    private static final String KAFKA_BROKER = "localhost:9092";

    public static void main(String[] args) {
        Pipeline p = buildPipeline();

        JetInstance jet = Jet.newJetClient();

        try {
            JobConfig jobConfig = new JobConfig()
                    .setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE)
                    .setName("Query1");

            jet.newJobIfAbsent(p, jobConfig);
        } finally {
            Jet.shutdownAll();
        }
    }

    private static Pipeline buildPipeline() {
        Pipeline p = Pipeline.create();

        StreamStage<Tuple2<Long, Trade>> aggregated =
                p.drawFrom(
                        KafkaSources.<String, String, Trade>kafka(
                        kafkaSourceProps(),
                        record -> new Trade(record.value()),
                        "trades")
                )
                 .withoutTimestamps()
                 .groupingKey(Trade::getSymbol)
                 .mapStateful(() -> new long[1], (state, key, trade) -> tuple2(++state[0], trade))
                .setName("sum by symbol");

        // add results
        StreamStage<Entry<String, Long>> symbolResult = aggregated
                .map(t -> {
                    Long value = t.f0();
                    Trade trade = t.f1();
                    return entry(trade.getSymbol(), value);
                })
                .setName("(symbol, result)");

        // write results to IMDG IMap
        symbolResult
                .drainTo(Sinks.map("query1_Results"));


        // write results to Kafka topic
//        symbolResult
//                .drainTo(KafkaSinks.kafka(kafkaSinkProps(), "query1_Results"));


        // add detail rows to IMap
        aggregated
                .map(t -> tuple2(t.f1().getSymbol(), t.f1())) // (symbol, trade)
                .drainTo(Sinks.<Tuple2<String, Trade>, String, List<Trade>>mapWithUpdating(
                        "query1_DrillDown", Tuple2::f0,
                        (list, t) -> {
                            if (list == null) {
                                list = new ArrayList<Trade>();
                            }
                            list.add(t.f1());
                            return list;
                        }));

        // add detail rows to multi map as (symbol, trade)
//        aggregated.drainTo(multiMapSink("query1_DrillDown", t -> t.f1().getSymbol(), t -> t.f1()));
        return p;
    }

    private static Properties kafkaSourceProps() {
        Properties props = new Properties();
        props.setProperty("auto.offset.reset", "earliest");
        props.setProperty("bootstrap.servers", KAFKA_BROKER);
        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", StringDeserializer.class.getName());
        return props;
    }

    private static Properties kafkaSinkProps() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", KAFKA_BROKER);
        props.setProperty("key.serializer", StringSerializer.class.getName());
        props.setProperty("value.serializer", LongSerializer.class.getName());
        return props;
    }

    private static <T, K, V> Sink<T> multiMapSink(String name, FunctionEx<T, K> keyFn, FunctionEx<T, V> valueFn) {
        return SinkBuilder.sinkBuilder("multiMapSink(" + name + ")", ctx -> ctx.jetInstance().getHazelcastInstance().<K, V>getMultiMap(name))
                .<T>receiveFn((mmap, t) -> {
                    mmap.put(keyFn.apply(t), valueFn.apply(t));
                }).preferredLocalParallelism(8).build();
    }

}
