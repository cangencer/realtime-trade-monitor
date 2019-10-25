import model.Trade;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public class TradeProducer {

    private static final int MAX_BATCH_SIZE = 16*1024;
    private static final int QUANTITY = 1_000;
    private static final String TOPIC = "trades";

    private final int rate;
    private final Map<String, Integer> tickerToPrice;
    private final KafkaProducer<String, String> producer;
    private final List<String> tickers;

    private long emitSchedule;

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.out.println("TradeMonitor <bootstrap servers> <rate>");
            System.exit(1);
        }
        String servers = args[0];
        int rate = Integer.parseInt(args[1]);
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", servers);
        props.setProperty("key.serializer", StringSerializer.class.getName());
        props.setProperty("value.serializer", StringSerializer.class.getName());

        new TradeProducer(props, rate, loadTickers()).run();
    }

    private TradeProducer(Properties props, int rate, List<String> tickers) {
        this.rate = rate;
        this.tickers = tickers;
        this.tickerToPrice  = tickers.stream().collect(Collectors.toMap(t -> t, t -> 2500));
        this.producer = new KafkaProducer<>(props);
        this.emitSchedule = System.nanoTime();
    }

    private void run() throws InterruptedException {
        System.out.println("Producing " + rate + " trades per second");
        while (true) {
            long interval = TimeUnit.SECONDS.toNanos(1) / rate;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < MAX_BATCH_SIZE; i++) {
                if (System.nanoTime() < emitSchedule) {
                    break;
                }
                String ticker = tickers.get(rnd.nextInt(tickers.size()));
                int price = tickerToPrice.compute(ticker, (t, v) -> v + rnd.nextInt(-1, 2));
                Trade trade = new Trade(
                        UUID.randomUUID().toString(),
                        System.currentTimeMillis(),
                        ticker,
                        rnd.nextInt(10, QUANTITY),
                        price
                );
                producer.send(new ProducerRecord<>(TOPIC, trade.getSymbol(), trade.toString()));
                emitSchedule += interval;
            }
            Thread.sleep(1);
        }
    }

    private static List<String> loadTickers() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                TradeProducer.class.getResourceAsStream("/nasdaqlisted.txt"), UTF_8))
        ) {
            return reader.lines()
                    .skip(1)
                    .map(l -> l.split("\\|")[0])
                    .collect(toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
