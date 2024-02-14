package com.example.tracingplayground.controller;

import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class SimpleController {

    private final StreamBridge streamBridge;

    private final Tracer tracer;

    @PostMapping("/")
    public Mono<Void> post(@RequestBody Map<String, Object> data) {
        try (BaggageInScope scope = tracer.createBaggageInScope("baggageTest", "baggageValue")) {
            log.info("Just set the baggage field...");

            streamBridge.send("output", MessageBuilder
                    .withPayload(Map.of("name", "hello from outside"))
                    .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                    .build());

            return Mono
                    .fromRunnable(() -> {
                        log.info("Hello from REST controller. About to publish a Kafka message...");
                        streamBridge.send("output", MessageBuilder
                                .withPayload(Map.of("name", "hello from Mono"))
                                .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                                .build());
                    })
                    .contextWrite(context -> context.put("baggageTest", "baggageValue"))
                    .then();
        }
    }

    @GetMapping("/")
    public Mono<String> get() {
        return Mono.just("Hello");
    }

}
