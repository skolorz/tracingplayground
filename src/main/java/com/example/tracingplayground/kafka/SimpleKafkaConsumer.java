package com.example.tracingplayground.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Slf4j
public class SimpleKafkaConsumer implements Consumer<Message<String>> {
    @Override
    public void accept(Message<String> message) {
        log.info(message.getHeaders().toString());
        log.info(message.getPayload());
    }
}
