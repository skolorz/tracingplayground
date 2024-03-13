package com.example.tracingplayground.kafka;

import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;


@Component
@Slf4j
public class SimpleKafkaConsumer implements Consumer<Message<String>> {
    private final StreamBridge streamBridge;

    private final WebClient webClient;
    private final ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder().build();

    public SimpleKafkaConsumer(StreamBridge streamBridge, WebClient.Builder builder) {
        this.streamBridge = streamBridge;
        this.webClient = builder.baseUrl("http://localhost:8080/").build();
    }

    @Override
    public void accept(Message<String> message) {
        publish(message)
                .flatMap(msg -> httpGet().thenReturn(msg))
                .flatMap(this::publish)
                .flatMap(msg -> httpGet().thenReturn(msg))
//                .contextCapture()
                .block();
    }

    private Mono<?> httpGet() {
        log.info("webClient.get");
        return webClient.get().retrieve().bodyToMono(String.class);
    }

    private Mono<Message<String>> publish(Message<String> message) {
        return Mono.deferContextual(contextView -> {
            this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
                    ObservationThreadLocalAccessor.KEY);
            log.info("Publishing message");
            streamBridge.send("echo2", MessageBuilder
                    .withPayload(Map.of("payload", message.getPayload()))
                    .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                    .build());
            return Mono.just(message);
        });

    }
}
