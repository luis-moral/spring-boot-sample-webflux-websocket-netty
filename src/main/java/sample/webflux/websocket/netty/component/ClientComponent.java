package sample.webflux.websocket.netty.component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import reactor.core.scheduler.Schedulers;
import sample.webflux.websocket.netty.handler.ClientWebSocketHandler;
import sample.webflux.websocket.netty.handler.WebSocketSessionHandler;

@Component
public class ClientComponent implements ApplicationListener<ApplicationReadyEvent>
{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ConfigurableApplicationContext applicationContext;
	
	@Autowired
	private WebSocketClient webSocketClient;
	
	@Autowired
	private ClientWebSocketHandler clientWebSocketHandler;
	
	@Value("${server.port}")
	private int serverPort;
	
	@Value("${sample.path}")
	private String samplePath;
	
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) 
	{		
		URI uri = null;
		
		try
		{
			uri = new URI("ws://localhost:" + serverPort + samplePath);
		}
		catch (URISyntaxException USe)
		{
			throw new IllegalArgumentException(USe);
		}
		
		Disposable clientConnection = 		
			webSocketClient
				.execute(uri, clientWebSocketHandler)
				.subscribeOn(Schedulers.elastic())
				.subscribe();
		
		WebSocketSessionHandler sessionHandler = 
			clientWebSocketHandler
				.connected()
				.doOnNext(id -> logger.info("Connected [{}]", id))
				.blockFirst();
		
		String sendMessage = "Test Message";
		
		sessionHandler.send(sendMessage);			
		logger.info("Client Sent: [{}]", sendMessage);
				
		sessionHandler
			.receive()
			.subscribeOn(Schedulers.elastic())
			.subscribe(message -> logger.info("Client Received: [{}]", message));		
		
		Mono
			.delay(Duration.ofSeconds(5))			
			.doOnNext(value -> clientConnection.dispose())
			.doOnNext(value -> logger.info("Disconnected."))
			.then(Mono.delay(Duration.ofMillis(500)))
			.map(value -> SpringApplication.exit(applicationContext, () -> 0))
			.subscribe(exitValue -> System.exit(exitValue));
	}
}
