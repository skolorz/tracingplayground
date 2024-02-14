package com.example.tracingplayground.controller;

import io.micrometer.context.ContextRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
public class SimpleController {

    private final StreamBridge streamBridge;

    private final Tracer tracer;
    private ContextRegistry registry = new ContextRegistry();

    public SimpleController(StreamBridge streamBridge, Tracer tracer) {
        this.streamBridge = streamBridge;
        this.tracer = tracer;
        registry.registerThreadLocalAccessor(new ObservationThreadLocalAccessor());
        ;
    }

    @PostMapping("/a")
    public Mono<Void> post(@RequestBody Map<String, Object> data) {

        return Mono
                .fromRunnable(() -> {
                    log.info("Sending message from mono");
                    streamBridge.send("output", MessageBuilder
                            .withPayload(Map.of("name", "hello from Mono"))
                            .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                            .build());
                })
                .contextWrite(context -> context.put("otherField", "from Mono context"))
                .then();
    }

    @PostMapping("/b")
    public Mono<Void> postWithBaggage(@RequestBody Map<String, Object> data) {
        try (BaggageInScope scope = tracer.createBaggageInScope("someField", "someValue")) {

            return Mono
                    .fromRunnable(() -> {
                        log.info("Sending message from mono");
                        streamBridge.send("output", MessageBuilder
                                .withPayload(Map.of("name", "hello from Mono"))
                                .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                                .build());
                    })
                    .contextWrite(context -> context.put("otherField", "from Mono context"))
                    .then()
                    .doFinally(s -> scope.close());
        }
    }

    @PostMapping("/c")
    public Mono<Void> postWithBaggage2(@RequestBody Map<String, Object> data) {

        return Mono.defer(() -> {
                    BaggageInScope scope = tracer.createBaggageInScope("someField", "someValue");
                    return Mono.just(scope)
                            .fromRunnable(() -> {
                                log.info("Sending message from mono");
                                streamBridge.send("output", MessageBuilder
                                        .withPayload(Map.of("name", "hello from Mono"))
                                        .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                                        .build());
                            })
                            .doFinally(s -> scope.close());
                })
                .then();
    }

    @PostMapping("/d")
    public Mono<Void> postWithBaggage3(@RequestBody Map<String, Object> data) {

        return Mono.defer(() -> {
                    BaggageInScope scope = tracer.createBaggageInScope("someField", "someValue");
                    return Mono.just(scope);
                })
                .fromRunnable(() -> {
                    log.info("Sending message from mono");
                    streamBridge.send("output", MessageBuilder
                            .withPayload(Map.of("name", "hello from Mono"))
                            .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                            .build());
                })
                .then();
    }

    @GetMapping("/")
    public Mono<String> get() {
        return Mono.just("Hello");
    }

}
