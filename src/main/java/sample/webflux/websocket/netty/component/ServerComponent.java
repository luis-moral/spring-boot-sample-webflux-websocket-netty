package sample.webflux.websocket.netty.component;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import sample.webflux.websocket.netty.handler.MessageDTO;
import sample.webflux.websocket.netty.handler.ServerWebSocketHandler;

@Component
public class ServerComponent implements ApplicationListener<ApplicationReadyEvent>
{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ServerWebSocketHandler serverWebSocketHandler;
	
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) 
	{
		Flux<MessageDTO> receiveAll =
			serverWebSocketHandler
				.receive()
				.subscribeOn(Schedulers.elastic())
				.doOnNext(message -> logger.info("Server Received: [{}]", message.getValue()));
		
		Mono<MessageDTO> receiveFirst =
			serverWebSocketHandler
				.receive()
				.subscribeOn(Schedulers.elastic())
				.next();
		
		Flux<MessageDTO> send =
			Flux
				.interval(Duration.ofMillis(500))
				.subscribeOn(Schedulers.elastic())
				.takeUntil(value -> !serverWebSocketHandler.isConnected())
				.map(interval -> new MessageDTO(interval))				
				.doOnNext(dto -> serverWebSocketHandler.send(dto))
				.doOnNext(dto -> logger.info("Server Sent: [{}]", dto.getValue()));
		
		serverWebSocketHandler
			.connected()
			.subscribe(value -> logger.info("Client connected."));
		
		receiveAll.subscribe();
		receiveFirst.thenMany(send).subscribe();
	}
}