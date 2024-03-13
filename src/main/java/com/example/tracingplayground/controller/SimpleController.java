package com.example.tracingplayground.controller;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
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
    private final ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder().build();
    private ContextRegistry registry = new ContextRegistry();

    @PostMapping("/a1")
    public Mono<Void> post1(@RequestBody Map<String, Object> data) {
        return Mono.deferContextual(contextView -> {
//            try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
//                    ObservationThreadLocalAccessor.KEY)) {
                log.info("POST baggage {}", tracer.getAllBaggage());
                log.info("Sending message from mono");
                return Mono.fromRunnable(() ->
                        streamBridge.send("output", MessageBuilder
                                .withPayload(Map.of("name", "hello from Mono"))
                                .setHeader(KafkaHeaders.KEY, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                                .build()));
//            }
        });
    }


    @GetMapping("/")
    public Mono<String> get() {
        return Mono.deferContextual(contextView -> {
//            try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(contextView,
//                    ObservationThreadLocalAccessor.KEY)) {


                log.info("GET baggage {}", tracer.getAllBaggage());
                return Mono.just("Hello");
//            }
        });
    }

}
