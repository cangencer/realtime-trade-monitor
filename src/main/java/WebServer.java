import com.hazelcast.jet.IMapJet;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import model.Trade;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * date: 2019-10-25
 * author: emindemirci
 */
public class WebServer {

    private static Map<String, WsContext> sessions = new ConcurrentHashMap<>();
    private static JetInstance jet = Jet.newJetClient();

    public static void main(String[] args) {

        IMapJet<String, Long> results = jet.getMap("query1_Results");
        IMapJet<String, Trade> trades = jet.getMap("trades");
        IMapJet<String, List<String>> drillDown = jet.getMap("query1_Trades");


        Javalin app = Javalin.create().start(9999);
        app.config
                .addStaticFiles("/app")                                     // The ReactJS application
                .addStaticFiles("/")                                        // Other static assets, external to the ReactJS application
                .addSinglePageRoot("/", "/app/index.html");   // Catch-all route for the single-page application

        app.ws("/trades", wsHandler -> {
            wsHandler.onConnect(ctx -> {
                String sessionId = ctx.getSessionId();
                System.out.println("Starting the session -> " + sessionId);
                sessions.put(sessionId, ctx);
            });

            wsHandler.onClose(ctx -> {
                String sessionId = ctx.getSessionId();
                System.out.println("Closing the session -> " + sessionId);
                sessions.remove(sessionId, ctx);
            });

            wsHandler.onMessage(ctx -> {
                String sessionId = ctx.getSessionId();
                String message = ctx.message();
                WsContext session = sessions.get(sessionId);

                if ("LOAD_TICKERS" .equals(message)) {
                    JSONObject jsonObject = new JSONObject();
                    Set<String> tickers = drillDown.keySet();
                    for (String ticker : tickers) {
                        jsonObject.append("tickers", new JSONObject().put("ticker", ticker));
                    }
                    session.send(jsonObject.toString());

                } else if (message.startsWith("DRILL_TICKER")) {
                    String ticker = message.split(" ")[1];
                    System.out.println("Session -> " + sessionId + " requested ticker -> " + ticker);
                    JSONObject jsonObject = new JSONObject();
                    drillDown.get(ticker).forEach(record -> {
                        Long value = results.get(ticker);
                        jsonObject.put("ticker", ticker);
                        jsonObject.put("count", value);
                        Trade trade = trades.get(record);
                        jsonObject.append("data", new JSONObject()
                                .put("id", trade.getId())
                                .put("time", trade.getTime())
                                .put("symbol", trade.getSymbol())
                                .put("quantity", trade.getQuantity())
                                .put("price", trade.getPrice()));
                    });
                    session.send(jsonObject.toString());
                }
            });
        });
    }

}
