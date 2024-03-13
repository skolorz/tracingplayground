package com.example.tracingplayground;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;

@Configuration
public class KafkaHeadersConfiguration {

    @Bean
    DefaultKafkaHeaderMapper kafkaCustomHeaderMapper(ObjectMapper objectMapper) {
        DefaultKafkaHeaderMapper defaultKafkaHeaderMapper = new DefaultKafkaHeaderMapper(objectMapper);
        defaultKafkaHeaderMapper.setEncodeStrings(true);
        return defaultKafkaHeaderMapper;
    }
}
