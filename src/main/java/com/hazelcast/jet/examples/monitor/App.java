package com.hazelcast.jet.examples.monitor;

import com.hazelcast.aggregation.Aggregators;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MapEvent;
import com.hazelcast.core.MultiMap;
import com.hazelcast.jet.IMapJet;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.examples.monitor.model.Trade;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) throws InterruptedException {
        JetInstance jet = Jet.newJetClient();

        while (true) {
            IMapJet<String, Long> results = jet.getMap("query1_Results");
            IMapJet<String, List<Trade>> drillDown = jet.getMap("query1_DrillDown");
//            MultiMap<String, Trade> drillDown = jet.getHazelcastInstance().getMultiMap("query1_DrillDown");


//            drillDown.addEntryListener(new EntryListener<String, Trade>() {
//                @Override
//                public void entryEvicted(EntryEvent<String, Trade> event) {
//
//                }
//
//                @Override
//                public void entryRemoved(EntryEvent<String, Trade> event) {
//
//                }
//
//                @Override
//                public void entryUpdated(EntryEvent<String, Trade> event) {
//
//                }
//
//                @Override
//                public void mapCleared(MapEvent event) {
//
//                }
//
//                @Override
//                public void mapEvicted(MapEvent event) {
//
//                }
//
//                @Override
//                public void entryAdded(EntryEvent<String, Trade> entryEvent) {
////                    System.out.println("New trade for AAPL: " + entryEvent.getValue());
//                }
//            }, "AAPL", true);

            long count = 0;
            long start = 0;
            while (true) {
                System.out.println("AAPL " + results.get("AAPL"));
                System.out.println("First 5 AAPL Trades:");
                Collection<Trade> trades = drillDown.get("AAPL");
                if (trades != null) {
                    trades.stream().limit(5).forEach(t -> {
                        System.out.println(t);
                    });
                }
                long prevCount = count;
                count = results.aggregate(Aggregators.longSum());
                long end = System.nanoTime();
                System.out.printf("Total trades so far: %,d%n", count);
                if (start > 0) {
                    long elapsed = TimeUnit.NANOSECONDS.toSeconds(end - start);
                    long recordCount = count - prevCount;
                    System.out.printf("Processed " + recordCount/(double)elapsed + " recs/s");
                }
                start = end;
                Thread.sleep(5000);
            }
        }
    }
}
