package org.billi;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.billi.data.SendMessage;
import org.billi.data.StreamsResponse;
import org.billi.data.Update;
import org.billi.markov.Markov;
import ratpack.exec.ExecController;
import ratpack.exec.ExecStarter;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.handling.Handler;
import ratpack.http.Headers;
import ratpack.http.TypedData;
import ratpack.http.client.HttpClient;
import ratpack.jackson.Jackson;
import ratpack.jackson.JsonRender;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.service.Service;
import ratpack.service.StartEvent;
import ratpack.service.StopEvent;

import javax.swing.text.AbstractDocument;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Main {
    private static String token = "telegram_bot_token";
    public static final String api = "https://api.telegram.org/bot" + token;
    private static String host = "127.0.0.1";
    private static int port = 3000;
    private static String urlPrefix = "https://your_domain/";
    private static String callback = "billi_callback_url";
    private static final String gen = "generate_quote_url";
    public static final String EMPTY = UUID.randomUUID().toString();
    public static final String DOC = "https://docs.google.com/document/d/1sV9fooOM39blGt2BZHfdqHCysyjrKmz-0WiWssaMy1Q/edit?usp=sharing";

    // "display_name":"billi777hard","_id":"110071481"
    public static final String STREAM_ID = "110071481";
    public static final String STREAM_STATUS_URL = "https://api.twitch.tv/kraken/streams?channel=" + STREAM_ID;
    public static final String CLIENT_ID = "twitch_app_client_id";    // Client-ID
    public static final String ACCEPT = "application/vnd.twitchtv.v5+json";

    private ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        Markov markov = new Markov();

        URL resource = Main.class.getResource("/quotes.txt");
        Path path = Paths.get(resource.toURI());
        List<String> strings = Files.readAllLines(path);

        strings.forEach(markov::add);

        Service watcher = new Service() {
            @Override
            public String getName() {
                return "Billi watcher service";
            }

            @Override
            public void onStart(StartEvent event) throws Exception {
                log.info("onStart");
                HttpClient client = event.getRegistry().get(HttpClient.class);
                client.get(URI.create(api + "/setWebhook?url=" + urlPrefix + callback))
                        .then(resp -> {
                            log.info(resp.getBody().getText());
                            client.get(URI.create(api + "/getWebhookInfo"))
                                    .then(resp2 -> log.info(resp2.getBody().getText()));
                        });
            }

            @Override
            public void onStop(StopEvent event) throws Exception {
                log.info("onStop");
                HttpClient client = event.getRegistry().get(HttpClient.class);
                client.get(URI.create(api + "/deleteWebhook"))
                        .then(resp -> log.info(resp.getBody().getText()));
            }
        };

        log.info("Started");

        Handler loggingHandler = ctx -> {
            Headers headers = ctx.getRequest().getHeaders();
            StringBuilder sb = new StringBuilder("Headers:\n");
            for (Map.Entry<String, String> header: headers.asMultiValueMap().entrySet()) {
                sb.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
            }
            ctx.getRequest().getBody().then(data -> {
                sb.append("Payload: ").append(data.getText());
                log.info("REQUEST: " + sb.toString());
                ctx.next(Registry.single(data));
            });
        };

        RatpackServer.start(server -> server
                .serverConfig(ServerConfig.builder().port(3000))
                .registryOf(r -> r.add(watcher))
                .handlers(chain -> chain
                        .all(loggingHandler)
                        .get(ctx -> ctx.render("Дратуте"))
                        .get(gen, ctx -> ctx.render(markov.generate()))
                        .post(callback, ctx -> {
                            log.info("on callback");
                            JsonNode update = ctx.parse(ctx.get(TypedData.class), Jackson.jsonNode());
                            String sender = update.at("/message/from/id").asText("");
                            String msg = update.at("/message/text").asText(EMPTY);
                            Promise<String> quote = Promise.value("Здарова! От души душа в душу, братуха! Команды:\n" +
                                    "катаешь? - узнать статус стрима\n" +
                                    "Напиши что угодно, чтобы получить в ответ цитату\n" +
                                    "Кидай цитаты сюда: " + DOC);
                            String lmsg = msg.toLowerCase();
                            log.info(lmsg);
                            if (lmsg.startsWith("привет") || lmsg.startsWith("/help")) {
                                // do nothing, send greeting
                            } else if (lmsg.startsWith("катаешь")) {
                                HttpClient client = ctx.get(HttpClient.class);
                                quote = client
                                        .get(URI.create(STREAM_STATUS_URL), req -> {
                                            req.getHeaders()
                                                    .set("Client-ID", CLIENT_ID)
                                                    .set("Accept", ACCEPT);
                                        })
                                        .map(resp -> {
                                            StreamsResponse streamsResponse = ctx.parse(resp.getBody(), Jackson.fromJson(StreamsResponse.class));
                                            if (streamsResponse.getStreams() != null) {
                                                StreamsResponse.TwitchStream live = streamsResponse.getStreams().stream()
                                                        .filter(s -> "live".equals(s.getStreamType()))
                                                        .findFirst().orElse(null);
                                                if (live != null) {
                                                    return "Катаю! Сейчас в " + live.getGame() +
                                                            (live.getChannel() == null? "" : " - " + live.getChannel().getStatus())
                                                            + "\nСмотри на https://www.twitch.tv/billi777hard";
                                                } else {
                                                    return "Сейчас оффлайн";
                                                }
                                            } else {
                                                return "Сейчас оффлайн";
                                            }
                                        });
                            } else if (!EMPTY.equals(msg)) {
                                quote = Promise.value(markov.generate());
                            }
                            quote
                                    .then(text -> {
                                JsonRender reply = Jackson.json(SendMessage.send(update.at("/message/chat/id").asText(), text));
                                log.info(" >>> " + text);
                                ctx.render(reply);
                            });
                        })
                )

        );
    }
}
