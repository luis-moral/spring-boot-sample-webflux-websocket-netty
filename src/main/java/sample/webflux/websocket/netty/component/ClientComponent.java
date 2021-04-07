package sample.webflux.websocket.netty.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import sample.webflux.websocket.netty.handler.ClientWebSocketHandler;
import sample.webflux.websocket.netty.logic.ClientLogic;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

@Component
public class ClientComponent implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private WebSocketClient webSocketClient;

    @Value("${server.port}")
    private int serverPort;

    @Value("${sample.path}")
    private String samplePath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ClientLogic clientLogic = new ClientLogic();
        Disposable logicOne = clientLogic.start(webSocketClient, getURI(), new ClientWebSocketHandler());
        Disposable logicTwo = clientLogic.start(webSocketClient, getURI(), new ClientWebSocketHandler());

        Mono
            .delay(Duration.ofSeconds(10))
            .subscribe(value -> {
                logicOne.dispose();
                logicTwo.dispose();

                SpringApplication.exit(applicationContext, () -> 0);
            });
    }

    private URI getURI() {
        try {
            return new URI("ws://localhost:" + serverPort + samplePath);
        } catch (URISyntaxException USe) {
            throw new IllegalArgumentException(USe);
        }
    }
}
